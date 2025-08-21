echo Deploying test suite 3...
rm -f ts-st-participant-vhl.zip
7z a ts-st-participant-vhl.zip .\ts-st-participant-vhl\*
curl -F updateSpecification=true -F specification=9E4C3D39XEB9CX4A5EX8D58XD8E8338E171D -F testSuite=@ts-st-participant-vhl.zip --header "ITB_API_KEY: 2E86828DXEDB9X4C5CX8D5DX5BF0A406DAB9" -X POST http://localhost:9003/api/rest/testsuite/deploy