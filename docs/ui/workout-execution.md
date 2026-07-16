# Workout Execution

The workout execution screen is focused on helping the user follow the planned gym session without leaving the app.

## Current capabilities

- Start and finish a training session.
- Register reps, load, RIR and RPE for each set.
- Show a visual checklist for planned sets.
- Uncheck a completed set and remove its linked local log while retaining the entered values.
- Keep completed set load and reps synchronized with history after later edits.
- Start a rest timer manually for each exercise.
- Automatically start the rest timer after a set is logged.
- Adjust active rest by minus or plus 30 seconds and play a notification cue at zero.
- Show total registered volume during the session.
- Show average RPE during the session.
- Open the exercise swap flow when a machine is busy or unavailable.
- Reorder exercises after a three-second hold while migrating every saved checklist by exercise.
- Calculate session progress from completed exercises rather than the currently selected index.
- Preserve the workout scroll position after set actions.

## Product rule

The checklist is driven by actual local set logs. A checked set means a matching record exists in `set_logs`, not only that the row is visually marked. Editing a checked row updates that record; unchecking it removes the record and returns the exercise to a pending state.
