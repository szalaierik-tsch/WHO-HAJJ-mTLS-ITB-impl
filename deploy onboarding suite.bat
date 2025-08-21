echo Deploying test suite 3...
rm -f ts-st-participant-onboarding.zip
7z a ts-st-participant-onboarding.zip .\ts-st-participant-onboarding\*
curl -F updateSpecification=true -F specification=513CD9B7XE8EDX4508X84EEX320C8F9022B5 -F testSuite=@ts-st-participant-onboarding.zip --header "ITB_API_KEY: 2E86828DXEDB9X4C5CX8D5DX5BF0A406DAB9" -X POST http://localhost:9003/api/rest/testsuite/deploy