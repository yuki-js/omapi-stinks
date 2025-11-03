# Issue Acceptance Criteria

This document outlines the acceptance criteria for each open issue. Each issue will be considered complete and ready to close when all criteria are met.

## Issue #7: ステータスバーを削除 (Remove Status Bar)

**Description**: Remove the status bar that displays "Module Active - 10 logs captured" or "Waiting for Hooks"

**Acceptance Criteria**:
- [ ] Status bar TextView removed from activity_main.xml layout
- [ ] Status update logic removed from MainActivity.java (updateStatus() calls)
- [ ] statusText field and related code cleaned up
- [ ] UI displays properly without the status bar
- [ ] App builds successfully (debug and release)
- [ ] No functional regressions

**Expected Outcome**: The app should no longer display a status bar at the top of the log list. The screen real estate will be freed up for more log entries.

---

## Issue #8: リフレッシュボタンは必要か？ (Is Refresh Button Needed?)

**Description**: Determine if the refresh button in the title bar is necessary, and remove if not needed

**Acceptance Criteria**:
- [ ] Analysis completed: auto-refresh runs every 1000ms making manual refresh redundant
- [ ] Refresh button removed from main_menu.xml
- [ ] action_refresh menu item handler removed from MainActivity
- [ ] action_refresh string resource cleaned up (optional)
- [ ] App builds successfully
- [ ] Auto-refresh continues to work properly

**Expected Outcome**: The refresh button should be removed from the toolbar since the app already auto-refreshes every second.

---

## Issue #9: フィルタバーはタイトルバーにしまう (Move Filter Bar to Title Bar)

**Description**: The filter bar takes up too much screen space. Move filter controls to the title bar with a button that opens a filter settings screen.

**Acceptance Criteria**:
- [ ] New filter button added to toolbar menu (main_menu.xml)
- [ ] Filter settings dialog/screen implemented with:
  - [ ] Search/text filter
  - [ ] Package filter
  - [ ] Function filter
  - [ ] Time range filter
- [ ] Current filter bar in activity_main.xml hidden or removed
- [ ] Filter state preserved when dialog is closed
- [ ] Visual indication when filters are active
- [ ] All existing filter functionality works
- [ ] App builds successfully
- [ ] UI is cleaner with more space for logs

**Expected Outcome**: The search bar and filter chips should be removed from the main screen. A filter button in the toolbar should open a dialog containing all filter controls.

---

## Issue #10: Log detail画面を強化 (Enhance Log Detail Screen)

**Description**: The current log detail screen is bland and lacks usability. Improve the design and add useful UI/UX features.

**Acceptance Criteria**:
- [ ] Improved visual design using Material Design components
- [ ] Better text formatting and readability
- [ ] Useful features added such as:
  - [ ] Copy buttons for APDU command, response, AID, etc.
  - [ ] Color coding or better visual hierarchy
  - [ ] Proper spacing and card layouts
  - [ ] Expandable/collapsible sections if needed
- [ ] Information is easier to read and use
- [ ] App builds successfully
- [ ] No regressions in functionality

**Expected Outcome**: The log detail screen should be more visually appealing and functional, with copy buttons and better formatting for easy use.

---

## Issue #11: エクスポート機能 (Export Functionality)

**Description**: Add an export button to the three-dot menu in the title bar. The export should respect current filters.

**Acceptance Criteria**:
- [ ] Export button added to main_menu.xml (in overflow menu)
- [ ] Export functionality implemented that:
  - [ ] Exports only filtered logs (respects current filter settings)
  - [ ] Supports a suitable format (JSON, CSV, or plain text)
  - [ ] Saves to user-accessible location or shares via Intent
- [ ] User can choose export format (if multiple formats supported)
- [ ] Export includes all relevant log entry fields
- [ ] Success/error feedback provided to user
- [ ] App builds successfully
- [ ] Permissions handled properly (if file writing needed)

**Expected Outcome**: Users should be able to export their filtered logs to a file for external analysis.

---

## Issue #12: Xposedコーリング情報をもっととる (Collect More Xposed Calling Information)

**Description**: CallLogEntry should capture more information. There's more data available from Xposed hooks that should be collected.

**Acceptance Criteria**:
- [ ] Analysis of available information from Xposed MethodHookParam
- [ ] New fields identified and added to CallLogEntry, such as:
  - [ ] Thread ID/name
  - [ ] Process ID
  - [ ] Stack trace (optional, for debugging)
  - [ ] Additional OMAPI method parameters
  - [ ] Method execution time/duration
  - [ ] Return values for non-transmit methods
- [ ] Hook implementations updated to capture new data
- [ ] LogBroadcaster updated to include new fields
- [ ] LogReceiver updated to parse new fields
- [ ] UI updated to display new information (MainActivity and LogDetailActivity)
- [ ] App builds successfully
- [ ] All hooks continue to work properly

**Expected Outcome**: Log entries should contain more comprehensive information about each OMAPI call, providing better debugging capabilities.

---

## Overall Testing Requirements

For all issues, the following must be verified:
- [ ] Debug build succeeds: `./gradlew assembleDebug`
- [ ] Release build succeeds: `./gradlew assembleRelease`
- [ ] Lint checks pass: `./gradlew lint` (or no new critical errors)
- [ ] App installs and runs without crashes
- [ ] No regressions in existing functionality
- [ ] Changes are minimal and focused on the issue requirements

---

## Note for Issue Reporter

Since I cannot directly comment on GitHub issues through the API, this document has been created in the repository. Please review these acceptance criteria and confirm if they align with your expectations. Any feedback can be incorporated before implementation begins.
