# GitHub Copilot Instructions for OMAPI Sniff

## Project Overview

This is an Android Xposed Module project that hooks and logs OMAPI (Open Mobile API) calls. The module intercepts calls to Secure Element APIs and displays them in a visual log viewer.

## Key Technologies

- **Language**: Java (Android)
- **Framework**: Xposed Framework
- **Build System**: Gradle 8.0
- **Min SDK**: Android 9.0 (API 28)
- **Target SDK**: Android 14 (API 34)

## Project Structure

```
omapi-sniff/
├── app/src/main/
│   ├── java/com/yukijs/omapisniff/
│   │   ├── XposedInit.java          # Main Xposed hook implementation
│   │   ├── CallLogger.java           # Thread-safe log storage
│   │   ├── MainActivity.java         # UI for viewing logs
│   │   └── LogAdapter.java           # RecyclerView adapter
│   ├── res/                          # Android resources
│   ├── assets/xposed_init           # Xposed entry point declaration
│   └── AndroidManifest.xml
├── build.gradle                      # Project-level build config
├── app/build.gradle                  # Module-level build config
└── .github/
    ├── workflows/                    # CI/CD workflows
    └── copilot/                      # Copilot agent configs
```

## Build Commands

### Essential Commands
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run lint checks
./gradlew lint

# Run all checks and build
./gradlew build
```

## Code Style Guidelines

### Java Conventions
- Use 4 spaces for indentation
- Follow Android Java code style
- Keep methods focused and concise
- Add Javadoc comments for public APIs

### Naming Conventions
- Classes: PascalCase (e.g., `CallLogger`)
- Methods: camelCase (e.g., `addLog`)
- Constants: UPPER_SNAKE_CASE (e.g., `MAX_LOGS`)
- Private fields: camelCase with descriptive names

### Xposed Hook Patterns
```java
XposedHelpers.findAndHookMethod(
    targetClass, 
    "methodName",
    parameterTypes,
    new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            // Pre-execution logic
        }
        
        @Override
        protected void afterHookedMethod(MethodHookParam param) {
            // Post-execution logic
        }
    }
);
```

## Critical Rules for Development

### Build Requirements
1. **ALWAYS** test both debug and release builds before completing work
2. **NEVER** commit code that doesn't compile
3. **ALWAYS** run lint checks and address critical issues
4. **NEVER** break existing functionality when adding features

### Xposed Module Requirements
1. All hook classes must be thread-safe
2. Never block the main thread in hooks
3. Always handle exceptions in hooks (log, don't crash)
4. Keep hook logic minimal and fast

### Android Best Practices
1. Follow Material Design guidelines for UI
2. Use RecyclerView for lists (already implemented)
3. Handle configuration changes properly
4. Clean up resources in lifecycle methods

## Testing Guidelines

### Manual Testing Checklist
- [ ] Module shows up in Xposed framework
- [ ] Module can be activated without errors
- [ ] UI launches and displays correctly
- [ ] Logs appear when OMAPI calls are made
- [ ] Clear and refresh functions work
- [ ] No crashes or ANRs

### Hook Testing
- Test with apps that use OMAPI (payment apps, SIM toolkit)
- Verify all method calls are captured
- Check hex formatting is correct
- Ensure timestamps are accurate

## Common Issues and Solutions

### Build Issues
- **Gradle version conflicts**: Check compatibility matrix
- **Missing SDK**: Install required Android SDK components
- **Dependency resolution**: Use `--refresh-dependencies`

### Xposed Issues
- **Hooks not working**: Check class/method names match target
- **ClassNotFoundException**: Target package may use different OMAPI implementation
- **Method signature mismatch**: Verify parameter types

### UI Issues
- **RecyclerView not scrolling**: Check layout_height settings
- **Logs not updating**: Verify CallLogger singleton is used correctly
- **Memory leaks**: Ensure proper lifecycle handling

## Dependencies

### Core Dependencies
```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.9.0'
implementation 'androidx.recyclerview:recyclerview:1.3.1'
compileOnly 'de.robv.android.xposed:api:82'
```

### Dependency Management
- Keep dependencies up to date but test after updates
- Use `compileOnly` for Xposed API (provided at runtime)
- Minimize dependencies to reduce APK size

## Documentation Standards

### Code Comments
- Explain WHY, not WHAT (code shows what)
- Document complex algorithms
- Add TODO comments for future improvements
- Include links to relevant specs (e.g., OMAPI spec)

### Git Commit Messages
```
<type>: <subject>

<body>

<footer>
```

Types: feat, fix, docs, style, refactor, test, chore

Example:
```
feat: Add support for new OMAPI methods

- Hook additional Session methods
- Add detailed logging for P2 parameter
- Update UI to display new log types

Closes #123
```

## Agent-Specific Instructions

### For Build Agents
- Focus on compilation and build success
- Address dependency issues
- Optimize build performance
- Ensure artifact generation

### For Code Review Agents
- Check for potential hook failures
- Verify thread safety
- Look for performance issues
- Ensure Android best practices

### For Documentation Agents
- Keep README and IMPLEMENTATION.md in sync
- Document new hooks added
- Update API coverage list
- Maintain changelog

## Important Notes

1. This is a debugging/analysis tool - prioritize logging completeness
2. Performance matters - hooks are in critical paths
3. Compatibility is key - support both old and new OMAPI packages
4. Security awareness - logs may contain sensitive data

## Resources

- [OMAPI Specification](https://globalplatform.org/wp-content/uploads/2018/04/GPD_Open_Mobile_API_Spec_v3.2.0.13_PublicReview.pdf)
- [Xposed Framework API](https://api.xposed.info/)
- [Android Developer Docs](https://developer.android.com/)
- [Material Design Guidelines](https://material.io/design)

## Success Criteria

A successful contribution should:
- Build successfully (both debug and release)
- Pass lint checks without critical errors
- Follow project code style
- Include appropriate documentation
- Not break existing functionality
- Be tested on target devices when possible
