@echo off
echo ============================================
echo  Harness Performance & Lint Check
echo ============================================
echo.

echo [1/2] Running Lint...
call gradlew lint
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  ERROR: Lint found issues. AI MUST fix before commit!
    echo  Do NOT skip. Rerun `gradlew lint` and check the report.
    echo.
    exit /b 1
)
echo Lint: PASSED
echo.

echo [2/2] Running Unit Tests...
call gradlew test
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo  WARNING: Tests failed. Review and fix.
    echo.
    exit /b 1
)
echo Tests: PASSED
echo.

echo ============================================
echo  All checks passed. Ready to commit.
echo ============================================
