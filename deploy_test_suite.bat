echo Deploying test suite 3...
rm -f hajj-test-suite.zip
7z a hajj-test-suite.zip .\hajj-test-suite\*
curl -F updateSpecification=true -F specification=D1FE2C22X5430X4A00X8F1EX55AD6F6669D9 -F testSuite=@hajj-test-suite.zip --header "ITB_API_KEY: 2E86828DXEDB9X4C5CX8D5DX5BF0A406DAB9" -X POST http://localhost:9003/api/rest/testsuite/deploy