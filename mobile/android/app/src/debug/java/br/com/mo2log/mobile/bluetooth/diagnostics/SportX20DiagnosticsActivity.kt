package br.com.mo2log.mobile.bluetooth.diagnostics

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import br.com.mo2log.mobile.BuildConfig
import br.com.mo2log.mobile.bluetooth.BluetoothPermissionController
import br.com.mo2log.mobile.ui.Mo2Colors
import br.com.mo2log.mobile.ui.Mo2Components
import br.com.mo2log.mobile.ui.Mo2Spacing
import br.com.mo2log.mobile.ui.mo2Dp

@SuppressLint("SetTextI18n")
class SportX20DiagnosticsActivity : Activity() {
    private lateinit var controller: SportX20DiagnosticsController
    private lateinit var statusText: TextView
    private lateinit var environmentText: TextView
    private lateinit var deviceText: TextView
    private lateinit var repositoryText: TextView
    private lateinit var gattText: TextView
    private lateinit var eventsText: TextView
    private lateinit var updateButton: Button
    private lateinit var loadingIndicator: ProgressBar
    private var pendingPermissionRefresh = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) {
            finish()
            return
        }
        setupContentView()
        val preferences = getSharedPreferences("mo2log_native", Context.MODE_PRIVATE)
        controller = SportX20DiagnosticsController(
            context = applicationContext,
            preferences = preferences,
            onStateChanged = ::renderState,
        )
        renderState(controller.currentState())
    }

    override fun onStart() {
        super.onStart()
        if (::controller.isInitialized) controller.start()
    }

    override fun onStop() {
        if (::controller.isInitialized) controller.stop()
        super.onStop()
    }

    override fun onDestroy() {
        if (::controller.isInitialized) controller.close()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != BluetoothPermissionController.REQUEST_CODE) return
        val granted = controller.hasConnectPermission()
        statusText.text = if (granted) {
            "Permissao concedida. Preparando leitura..."
        } else {
            "Permissao recusada. O diagnostico permanece somente informativo."
        }
        if (granted && pendingPermissionRefresh) {
            pendingPermissionRefresh = false
            controller.start()
            requestDiagnosticRefresh()
        }
    }

    private fun setupContentView() {
        window.statusBarColor = Mo2Colors.Background
        window.navigationBarColor = Mo2Colors.Background

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Mo2Colors.Background)
        scroll.isFillViewport = true

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(Mo2Spacing.Xxl), dp(Mo2Spacing.Xxl), dp(Mo2Spacing.Xxl), dp(Mo2Spacing.Xxl))
        scroll.addView(
            content,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            ),
        )

        val backButton = Mo2Components.actionButton(
            this,
            "Voltar",
            Mo2Colors.SurfaceAlt,
            Mo2Colors.TextPrimary,
        )
        backButton.setOnClickListener { finish() }
        content.addView(backButton, fullWidthParams())

        content.addView(Mo2Components.kicker(this, "FERRAMENTA DEBUG"))
        content.addView(
            Mo2Components.label(
                this,
                "Diagnostico Soundcore",
                Mo2Colors.TextPrimary,
                28f,
                true,
            ),
        )
        content.addView(
            Mo2Components.label(
                this,
                "Coleta local e somente leitura. UUID desconhecido nunca vira bateria automaticamente.",
                Mo2Colors.TextSecondary,
                14f,
                false,
            ),
        )

        statusText = Mo2Components.label(
            this,
            "Pronto para coletar evidencias.",
            Mo2Colors.TextSecondary,
            14f,
            false,
        )
        val statusCard = Mo2Components.card(this, Mo2Colors.SurfaceElevated)
        statusCard.orientation = LinearLayout.VERTICAL
        statusCard.addView(statusText)
        loadingIndicator = ProgressBar(this)
        loadingIndicator.isIndeterminate = true
        loadingIndicator.visibility = View.GONE
        val loadingParams = LinearLayout.LayoutParams(dp(28), dp(28))
        loadingParams.gravity = Gravity.CENTER_HORIZONTAL
        loadingParams.setMargins(0, dp(Mo2Spacing.Md), 0, 0)
        statusCard.addView(loadingIndicator, loadingParams)
        content.addView(statusCard)

        updateButton = Mo2Components.actionButton(this, "Atualizar leitura")
        updateButton.setOnClickListener { requestDiagnosticRefresh() }
        content.addView(updateButton, fullWidthParams())

        val secondaryActions = LinearLayout(this)
        secondaryActions.orientation = LinearLayout.HORIZONTAL
        val clearButton = Mo2Components.actionButton(
            this,
            "Limpar diagnostico",
            Mo2Colors.SurfaceAlt,
            Mo2Colors.TextPrimary,
        )
        clearButton.setOnClickListener {
            controller.clearTemporaryEvents()
            statusText.text = "Eventos temporarios removidos. Cache e configuracoes preservados."
        }
        secondaryActions.addView(clearButton, weightedButtonParams(endMargin = dp(4)))
        val copyButton = Mo2Components.actionButton(
            this,
            "Copiar diagnostico",
            Mo2Colors.SurfaceAlt,
            Mo2Colors.TextPrimary,
        )
        copyButton.setOnClickListener { copyDiagnostic() }
        secondaryActions.addView(copyButton, weightedButtonParams(startMargin = dp(4)))
        content.addView(secondaryActions, fullWidthParams())

        environmentText = addDiagnosticSection(content, "Ambiente")
        deviceText = addDiagnosticSection(content, "Dispositivo")
        repositoryText = addDiagnosticSection(content, "Estado do repository")
        gattText = addDiagnosticSection(content, "Servicos, caracteristicas e leituras GATT")
        eventsText = addDiagnosticSection(content, "Eventos")

        applySystemInsets(scroll, content)
        setContentView(scroll)
    }

    private fun requestDiagnosticRefresh() {
        when (controller.refreshAndInspect()) {
            DiagnosticRefreshRequest.STARTED -> {
                statusText.text = "Inspecao GATT em andamento..."
            }
            DiagnosticRefreshRequest.PERMISSION_REQUIRED -> {
                val permissions = controller.requiredPermissions()
                if (permissions.isNotEmpty()) {
                    pendingPermissionRefresh = true
                    statusText.text = "Conceda Dispositivos proximos para continuar."
                    requestPermissions(permissions, BluetoothPermissionController.REQUEST_CODE)
                } else {
                    statusText.text = "Permissao Bluetooth indisponivel."
                }
            }
            DiagnosticRefreshRequest.BLUETOOTH_UNAVAILABLE -> {
                statusText.text = "Este aparelho nao possui Bluetooth disponivel."
            }
            DiagnosticRefreshRequest.BLUETOOTH_DISABLED -> {
                statusText.text = "Ligue o Bluetooth e tente novamente."
            }
            DiagnosticRefreshRequest.DEVICE_NOT_FOUND -> {
                statusText.text = "Sport X20 pareado nao encontrado."
            }
            DiagnosticRefreshRequest.MULTIPLE_CANDIDATES -> {
                statusText.text = "Mais de um Sport X20 encontrado. Nenhum foi escolhido arbitrariamente."
            }
        }
    }

    private fun renderState(state: SportX20DiagnosticScreenState) {
        val environment = state.environment
        val device = state.device
        val repository = state.repositoryState
        updateButton.isEnabled = !state.isLoading
        updateButton.text = if (state.isLoading) "Lendo..." else "Atualizar leitura"
        loadingIndicator.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        if (!state.isLoading && state.lastError != null) {
            statusText.text = "Leitura encerrada: ${state.lastError}"
        } else if (!state.isLoading && state.readings.isNotEmpty()) {
            statusText.text = "Leitura concluida. Revise e copie as evidencias."
        }

        environmentText.text = buildString {
            appendLine("Mo2 Log: ${diagnosticValue(environment.appVersion)}")
            appendLine("Android: ${diagnosticValue(environment.androidVersion)} | API ${environment.androidApi}")
            appendLine("Celular: ${diagnosticValue(environment.manufacturer)} ${diagnosticValue(environment.phoneModel)}")
            appendLine("Bluetooth disponivel: ${diagnosticValue(environment.bluetoothAvailable)}")
            appendLine("Bluetooth ligado: ${diagnosticValue(environment.bluetoothEnabled)}")
            appendLine("BLUETOOTH_CONNECT: ${diagnosticValue(environment.connectPermissionGranted)}")
            append("Inicio: ${formatDiagnosticTimestamp(environment.startedAt)}")
        }
        deviceText.text = buildString {
            appendLine("Nome: ${diagnosticValue(device.name)}")
            appendLine("Alias: ${diagnosticValue(device.alias)}")
            appendLine("Endereco: ${diagnosticValue(device.maskedAddress)}")
            appendLine("Tipo: ${diagnosticValue(device.deviceType)}")
            appendLine("Classe: ${diagnosticValue(device.bluetoothClass)}")
            appendLine("Pareamento: ${diagnosticValue(device.bondState)}")
            appendLine("Transporte: ${diagnosticValue(device.transport)}")
            appendLine("A2DP: ${diagnosticValue(device.a2dpState)}")
            appendLine("Headset: ${diagnosticValue(device.headsetState)}")
            appendLine("ACL: ${diagnosticValue(device.aclState)}")
            appendLine("Candidatos: ${device.candidateCount}")
            appendLine("Selecao: ${device.selectionOrigin}")
            device.candidates.forEach { candidate ->
                appendLine("- ${diagnosticValue(candidate.name)} | ${candidate.maskedAddress}")
            }
        }.trimEnd()
        repositoryText.text = buildString {
            appendLine("connectionStatus: ${repository.connectionStatus}")
            appendLine("leftBatteryPercent: ${diagnosticValue(repository.leftBatteryPercent)}")
            appendLine("rightBatteryPercent: ${diagnosticValue(repository.rightBatteryPercent)}")
            appendLine("combinedBatteryPercent: ${diagnosticValue(repository.combinedBatteryPercent)}")
            appendLine("caseBatteryPercent: ${diagnosticValue(repository.caseBatteryPercent)}")
            appendLine("caseBatteryRange: ${diagnosticValue(repository.caseBatteryRange)}")
            appendLine("lastUpdatedAt: ${formatDiagnosticTimestamp(repository.lastUpdatedAt)}")
            appendLine("dataSource: ${repository.dataSource}")
            appendLine("isStale: ${repository.isStale}")
            append("erro: ${diagnosticValue(repository.errorMessage)}")
        }
        gattText.text = buildString {
            if (state.services.isEmpty()) appendLine(DIAGNOSTIC_UNAVAILABLE)
            state.services.forEach { service ->
                appendLine("SERVICO ${service.uuid}")
                service.characteristics.forEach { characteristic ->
                    appendLine("  CHAR ${characteristic.uuid}")
                    appendLine("  ${characteristic.propertyNames.joinToString(", ")}")
                    characteristic.descriptors.forEach { descriptor ->
                        appendLine("  DESC ${descriptor.uuid}")
                    }
                }
            }
            if (state.readings.isNotEmpty()) appendLine("\nLEITURAS")
            state.readings.forEach { reading ->
                appendLine("${formatDiagnosticTimestamp(reading.timestamp)} ${reading.characteristicUuid}")
                appendLine("  status=${reading.status}")
                appendLine("  hex=${diagnosticValue(reading.rawHex)}")
                appendLine("  decimal=${diagnosticValue(reading.decimalValues)}")
                appendLine("  interpretacao=${reading.interpretation}")
            }
        }.trimEnd()
        eventsText.text = if (state.events.isEmpty()) {
            DIAGNOSTIC_UNAVAILABLE
        } else {
            state.events.joinToString("\n") { event ->
                "${formatDiagnosticTimestamp(event.timestamp)} | ${event.category} | ${event.message}"
            }
        }
    }

    private fun addDiagnosticSection(parent: LinearLayout, title: String): TextView {
        parent.addView(Mo2Components.sectionHeader(this, title))
        val card = Mo2Components.card(this, Mo2Colors.SurfaceAlt)
        card.orientation = LinearLayout.VERTICAL
        val text = Mo2Components.label(
            this,
            DIAGNOSTIC_UNAVAILABLE,
            Mo2Colors.TextPrimary,
            12f,
            false,
        )
        text.typeface = Typeface.MONOSPACE
        text.setTextIsSelectable(true)
        card.addView(text)
        parent.addView(card)
        return text
    }

    private fun copyDiagnostic() {
        val report = controller.copyableReport()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostico Soundcore Sport X20", report))
        Toast.makeText(this, "Diagnostico copiado", Toast.LENGTH_SHORT).show()
    }

    @Suppress("DEPRECATION")
    private fun applySystemInsets(page: View, content: LinearLayout) {
        page.setOnApplyWindowInsetsListener { _, insets ->
            val top = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.statusBars()).top
            } else {
                insets.systemWindowInsetTop
            }
            val bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            } else {
                insets.systemWindowInsetBottom
            }
            content.setPadding(dp(Mo2Spacing.Xxl), dp(Mo2Spacing.Xxl) + top, dp(Mo2Spacing.Xxl), dp(Mo2Spacing.Xxl) + bottom)
            insets
        }
        page.requestApplyInsets()
    }

    private fun fullWidthParams(): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { setMargins(0, dp(Mo2Spacing.Sm), 0, dp(Mo2Spacing.Sm)) }
    }

    private fun weightedButtonParams(
        startMargin: Int = 0,
        endMargin: Int = 0,
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
            setMargins(startMargin, dp(Mo2Spacing.Sm), endMargin, dp(Mo2Spacing.Sm))
        }
    }

    private fun dp(value: Int): Int = mo2Dp(value)
}
