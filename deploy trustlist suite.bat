echo Deploying test suite 3...
rm -f ts-st-participant-trustlist.zip
7z a ts-st-participant-trustlist.zip .\ts-st-participant-trustlist\*
curl -F updateSpecification=true -F specification=2B7CAD26X5AC6X40E8X9D73X4CA08073513A -F testSuite=@ts-st-participant-trustlist.zip --header "ITB_API_KEY: 2E86828DXEDB9X4C5CX8D5DX5BF0A406DAB9" -X POST http://localhost:9003/api/rest/testsuite/deploy