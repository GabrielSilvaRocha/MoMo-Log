package br.com.mo2log.mobile.bluetooth.diagnostics

import br.com.mo2log.mobile.bluetooth.StandardGattBatteryProtocol
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val fullBluetoothAddressPattern = Regex("(?i)([0-9a-f]{2}:){5}[0-9a-f]{2}")

fun maskBluetoothAddress(address: String?): String {
    val value = address?.trim().orEmpty()
    if (!fullBluetoothAddressPattern.matches(value)) return DIAGNOSTIC_UNAVAILABLE
    val parts = value.uppercase(Locale.ROOT).split(":")
    return "${parts[0]}:${parts[1]}:**:**:${parts[4]}:${parts[5]}"
}

fun maskBluetoothAddressesInText(text: String): String {
    return fullBluetoothAddressPattern.replace(text) { match -> maskBluetoothAddress(match.value) }
}

fun diagnosticValue(value: Any?): String {
    return when (value) {
        null -> DIAGNOSTIC_UNAVAILABLE
        is String -> value.takeIf { it.isNotBlank() } ?: DIAGNOSTIC_UNAVAILABLE
        is Boolean -> if (value) "Sim" else "Nao"
        else -> value.toString()
    }
}

fun diagnosticGattPropertyNames(properties: Int): List<String> {
    val known = listOf(
        0x02 to "READ",
        0x08 to "WRITE",
        0x04 to "WRITE_NO_RESPONSE",
        0x10 to "NOTIFY",
        0x20 to "INDICATE",
        0x01 to "BROADCAST",
        0x40 to "SIGNED_WRITE",
        0x80 to "EXTENDED_PROPS",
    )
    val names = known.filter { (mask, _) -> properties and mask != 0 }.map { it.second }.toMutableList()
    val knownMask = known.fold(0) { acc, (mask, _) -> acc or mask }
    val unknownMask = properties and knownMask.inv()
    if (unknownMask != 0) names += "UNKNOWN(0x${unknownMask.toString(16).uppercase(Locale.ROOT)})"
    if (names.isEmpty()) names += "NONE"
    return names
}

fun diagnosticHex(payload: ByteArray?): String? {
    if (payload == null) return null
    if (payload.isEmpty()) return "(vazio)"
    return payload.joinToString(" ") { byte -> "%02X".format(Locale.ROOT, byte.toInt() and 0xff) }
}

fun diagnosticDecimal(payload: ByteArray?): String? {
    if (payload == null) return null
    if (payload.isEmpty()) return "[]"
    return payload.joinToString(prefix = "[", postfix = "]") { byte ->
        (byte.toInt() and 0xff).toString()
    }
}

fun interpretGattValue(characteristicUuid: String, payload: ByteArray?): DiagnosticInterpretation {
    val isOfficialBatteryLevel = characteristicUuid.equals(
        StandardGattBatteryProtocol.BATTERY_LEVEL_UUID.toString(),
        ignoreCase = true,
    )
    if (!isOfficialBatteryLevel) {
        return DiagnosticInterpretation("DESCONHECIDA", null)
    }
    val percent = StandardGattBatteryProtocol.parseBatteryLevel(payload)
    return if (percent == null) {
        DiagnosticInterpretation("BATTERY_LEVEL_OFICIAL_INVALIDO", null)
    } else {
        DiagnosticInterpretation("BATTERY_LEVEL_OFICIAL ($percent%)", percent)
    }
}

fun formatDiagnosticTimestamp(timestamp: Long?): String {
    if (timestamp == null) return DIAGNOSTIC_UNAVAILABLE
    return SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale("pt", "BR")).format(Date(timestamp))
}

fun formatSportX20DiagnosticReport(state: SportX20DiagnosticScreenState): String {
    val output = StringBuilder()
    val environment = state.environment
    val device = state.device
    val repository = state.repositoryState

    output.appendLine("MO2 LOG - DIAGNOSTICO SOUNDCORE SPORT X20")
    output.appendLine("Gerado em: ${formatDiagnosticTimestamp(System.currentTimeMillis())}")
    output.appendLine()
    output.appendLine("[AMBIENTE]")
    output.appendLine("Mo2 Log: ${diagnosticValue(environment.appVersion)}")
    output.appendLine("Android: ${diagnosticValue(environment.androidVersion)} (API ${environment.androidApi})")
    output.appendLine("Celular: ${diagnosticValue(environment.manufacturer)} ${diagnosticValue(environment.phoneModel)}")
    output.appendLine("Bluetooth disponivel: ${diagnosticValue(environment.bluetoothAvailable)}")
    output.appendLine("Bluetooth ligado: ${diagnosticValue(environment.bluetoothEnabled)}")
    output.appendLine("BLUETOOTH_CONNECT: ${diagnosticValue(environment.connectPermissionGranted)}")
    output.appendLine("Inicio: ${formatDiagnosticTimestamp(environment.startedAt)}")
    output.appendLine()
    output.appendLine("[DISPOSITIVO]")
    output.appendLine("Nome: ${diagnosticValue(device.name)}")
    output.appendLine("Alias: ${diagnosticValue(device.alias)}")
    output.appendLine("Endereco: ${diagnosticValue(device.maskedAddress)}")
    output.appendLine("Tipo: ${diagnosticValue(device.deviceType)}")
    output.appendLine("Classe: ${diagnosticValue(device.bluetoothClass)}")
    output.appendLine("Pareamento: ${diagnosticValue(device.bondState)}")
    output.appendLine("Transporte: ${diagnosticValue(device.transport)}")
    output.appendLine("A2DP: ${diagnosticValue(device.a2dpState)}")
    output.appendLine("Headset: ${diagnosticValue(device.headsetState)}")
    output.appendLine("ACL: ${diagnosticValue(device.aclState)}")
    output.appendLine("Candidatos: ${device.candidateCount}")
    output.appendLine("Origem da selecao: ${device.selectionOrigin}")
    device.candidates.forEach { candidate ->
        output.appendLine("- ${diagnosticValue(candidate.name)} | ${candidate.maskedAddress}")
    }
    output.appendLine()
    output.appendLine("[ESTADO DO REPOSITORY]")
    output.appendLine("connectionStatus: ${repository.connectionStatus}")
    output.appendLine("leftBatteryPercent: ${diagnosticValue(repository.leftBatteryPercent)}")
    output.appendLine("rightBatteryPercent: ${diagnosticValue(repository.rightBatteryPercent)}")
    output.appendLine("combinedBatteryPercent: ${diagnosticValue(repository.combinedBatteryPercent)}")
    output.appendLine("caseBatteryPercent: ${diagnosticValue(repository.caseBatteryPercent)}")
    output.appendLine("caseBatteryRange: ${diagnosticValue(repository.caseBatteryRange)}")
    output.appendLine("lastUpdatedAt: ${formatDiagnosticTimestamp(repository.lastUpdatedAt)}")
    output.appendLine("dataSource: ${repository.dataSource}")
    output.appendLine("isStale: ${repository.isStale}")
    output.appendLine("erro: ${diagnosticValue(repository.errorMessage)}")
    output.appendLine()
    output.appendLine("[SERVICOS GATT]")
    if (state.services.isEmpty()) output.appendLine(DIAGNOSTIC_UNAVAILABLE)
    state.services.forEach { service ->
        output.appendLine("Servico ${service.uuid}")
        service.characteristics.forEach { characteristic ->
            output.appendLine("  Caracteristica ${characteristic.uuid}")
            output.appendLine("  Propriedades: ${characteristic.propertyNames.joinToString(", ")}")
            characteristic.descriptors.forEach { descriptor ->
                output.appendLine("  Descritor: ${descriptor.uuid}")
            }
        }
    }
    output.appendLine()
    output.appendLine("[LEITURAS]")
    if (state.readings.isEmpty()) output.appendLine(DIAGNOSTIC_UNAVAILABLE)
    state.readings.forEach { reading ->
        output.appendLine("${formatDiagnosticTimestamp(reading.timestamp)} | ${reading.characteristicUuid}")
        output.appendLine("Servico: ${reading.serviceUuid}")
        output.appendLine("Status: ${reading.status}")
        output.appendLine("Hex: ${diagnosticValue(reading.rawHex)}")
        output.appendLine("Decimal: ${diagnosticValue(reading.decimalValues)}")
        output.appendLine("Interpretacao: ${reading.interpretation}")
    }
    output.appendLine()
    output.appendLine("[EVENTOS]")
    if (state.events.isEmpty()) output.appendLine(DIAGNOSTIC_UNAVAILABLE)
    state.events.sortedWith(compareBy(DiagnosticEvent::timestamp, DiagnosticEvent::sequence)).forEach { event ->
        output.appendLine(
            "${formatDiagnosticTimestamp(event.timestamp)} | ${event.category} | ${event.message}",
        )
    }
    output.appendLine()
    output.appendLine("Erro atual: ${diagnosticValue(state.lastError)}")
    output.appendLine("AVISO: UUIDs desconhecidos permanecem com interpretacao DESCONHECIDA.")
    return maskBluetoothAddressesInText(output.toString().trimEnd())
}
