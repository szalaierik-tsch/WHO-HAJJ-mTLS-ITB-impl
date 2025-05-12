import requests

def test_get_ifconfig_me():
    url = "https://ifconfig.me"
    response = requests.get(url)
    assert response.status_code == 200
    print(response.headers)

test_get_ifconfig_me()