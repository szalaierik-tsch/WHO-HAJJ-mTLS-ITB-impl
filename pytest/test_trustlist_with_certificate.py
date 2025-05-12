import requests

def test_get_trustlist_with_client_cert():
    url = "https://tng-dev.who.int/trustList"
    #url = "https://localhost:8443"
    
    # Path to your certificate and private key
    cert_file = "C:\\Development\\WHOProjects\\ExampleKeys\\generated\\TLS-XA-2025.pem"
    key_file = "C:\\Development\\WHOProjects\\ExampleKeys\\generated\\TLS-XA-2025.key"
    
    # Tuple format: (cert, key)
    cert = (cert_file, key_file)
    
    # Perform the GET request
    response = requests.get(url, cert=cert, verify=True)  # or verify=False to skip server cert validation (not recommended)
    print(response, type(response))
    print(response.text)

    # Example assertions
    assert response.status_code == 200

test_get_trustlist_with_client_cert()