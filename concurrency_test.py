
import requests
from concurrent.futures import ThreadPoolExecutor

URL = "http://localhost:8080/api/trips/3/seats/3/hold"
SESSION_COOKIE =  "1539EEC3ACBFB7A64E2282AC6EE89709"
def hold_seat(_):
    try:
        response = requests.post(
            URL,
            cookies={"JSESSIONID": SESSION_COOKIE},
            timeout=10
        )
        return response.status_code, response.text
    except requests.RequestException as e:
        return "ERROR", str(e)

with ThreadPoolExecutor(max_workers=2) as executor:
    results = list(executor.map(hold_seat, range(2)))

for i, (status, body) in enumerate(results, 1):
    print(f"\nRequest {i}: HTTP {status}")
    print(body)

print("\nExpected: one 200 and one 409.")
