import base45
import zlib
import cbor2
import pycose

hc1 = "HC1:6BF2Y2:9Q6WQC403DEJMF7+0E%3:8NBGGLVU8+A+S2FQR5Q91P985J4X9TMK3F97.ST+KI:P2BR9YBHOFOPM68FN L 24--U8-0OJFSFI5BFT54PDWVGN-BOFOV0HFH:NRK8S.DMMS7$2I*H:7NT4GZ8R$%N$2UYOTU+NA/DK 0CATN5T6N1M7N SJKUS-KTHBTS.LDZ00YJP2FY%HTD5D:B POJ3505WRJC4%I*G1+NB36B.6WBWPHOMUYAH/S:82SGMMDJLOEZ47PGH6N0YO0W*0PC2IVS0EG WCP6GNLARXHUM9*ZJ93JMDKNC5BWCJZF+4P3-FNI80PMN8JD3MNCNX/P2KRAQSZ CCNHDCQWJNCBL591U4NUJ4XF1UFIWTU*4T*-90MRO.S%YKPNHW Q/TFT1B*D1ZGT/80P89-8AS92%Q8EEMZK9MBR.RQT.3B/AA4UJRA*R2IPICS8412O:B1T0WLJ4*KQHCV7O9SEJ2ACNDI6BA80W1K3AHUXUT-HGGPG6O1%2JX38OT-B2$RS*0IV12BHD+3M0ANR-HF9RXAVPASN:39+5E6G$1LN2EB*HR0O WVR8OE3AA6W%CBT5G.ZU 03GKFX+EWOR SJ:VO DTI.HTXT97Q 2EP7WN3U:CV.BSQRK"

# 1. Strip HC1:
data = hc1[4:]

# 2. Base45 decode
decoded = base45.b45decode(data)
print("decoded=", decoded)
# 3. zlib decompress
decompressed = zlib.decompress(decoded)
print("decompressed=", decompressed)
# 4. Parse COSE (CBOR structure)
cose = cbor2.loads(decompressed)

print("cose=", cose)
# Sometimes cose is a CBORTag, unwrap it
if hasattr(cose, "value"):
    cose = cose.value

if not isinstance(cose, list) or len(cose) != 4:
    raise ValueError(f"Unexpected COSE structure: {type(cose)} → {cose}")
# COSE is a list: [protected header, unprotected header, payload, signature]
protected, unprotected, payload, signature = cose
print("protected=", cbor2.loads(protected))
print("keyID=", )
print("unprotected=", unprotected)
print("signature=", cbor2.loads(signature))
print("-------------------------")

# 5. Decode payload (CBOR again!)
cert = cbor2.loads(payload)

## Header contains the key ID. This can be used to download a key/certificate from DID
## GET DID, extract key with KID
## Verify signature with cose, input is the key downloaded

print("cert=", cert)

print(pycose.algorithms)
print(pycose.utils)
