import random
import string

def generate_negative_hc1_variants(valid_hc1, count=10):
    """
    Generate a list of negative HC1 test cases from a valid HC1 string.
    
    :param valid_hc1: str, a valid HC1 string starting with 'HC1:'
    :param count: int, number of variants to generate
    :return: list of str, negative HC1 variants
    """
    variants = []
    
    for _ in range(count):
        variant = valid_hc1
        
        # Pick a corruption type
        corruption_type = random.choice([
            "truncate", "invalid_char", "prefix", "shuffle", "duplicate"
        ])
        
        if corruption_type == "truncate":
            # Truncate the string randomly
            cut = random.randint(5, len(variant) - 5)
            variant = variant[:cut]
        
        elif corruption_type == "invalid_char":
            # Replace random chars with invalid ones
            num_changes = random.randint(1, 5)
            variant_list = list(variant)
            for _ in range(num_changes):
                idx = random.randint(0, len(variant_list) - 1)
                variant_list[idx] = random.choice(string.punctuation)
            variant = "".join(variant_list)
        
        elif corruption_type == "prefix":
            # Change the HC1 prefix to something invalid
            variant = "XC1:" + variant[4:]
        
        elif corruption_type == "shuffle":
            # Shuffle part of the string
            split_point = random.randint(10, len(variant) - 10)
            part = list(variant[split_point:])
            random.shuffle(part)
            variant = variant[:split_point] + "".join(part)
        
        elif corruption_type == "duplicate":
            # Duplicate a random slice of the string
            start = random.randint(0, len(variant) - 10)
            end = start + random.randint(1, 10)
            variant = variant[:end] + variant[start:end] + variant[end:]
        
        variants.append(variant)
    
    return variants


# Example usage
valid_hc1 = "HC1:6BF2Y2:9Q6WQC403DEJMF7+0E%3:8NBGGLVU8+A+S2FQR5Q91P985J4X9TMK3F97.ST+KI:P2BR9YBHOFOPM68FN L 24--U8-0OJFSFI5BFT54PDWVGN-BOFOV0HFH:NRK8S.DMMS7$2I*H:7NT4GZ8R$%N$2UYOTU+NA/DK 0CATN5T6N1M7N SJKUS-KTHBTS.LDZ00YJP2FY%HTD5D:B POJ3505WRJC4%I*G1+NB36B.6WBWPHOMUYAH/S:82SGMMDJLOEZ47PGH6N0YO0W*0PC2IVS0EG WCP6GNLARXHUM9*ZJ93JMDKNC5BWCJZF+4P3-FNI80PMN8JD3MNCNX/P2KRAQSZ CCNHDCQWJNCBL591U4NUJ4XF1UFIWTU*4T*-90MRO.S%YKPNHW Q/TFT1B*D1ZGT/80P89-8AS92%Q8EEMZK9MBR.RQT.3B/AA4UJRA*R2IPICS8412O:B1T0WLJ4*KQHCV7O9SEJ2ACNDI6BA80W1K3AHUXUT-HGGPG6O1%2JX38OT-B2$RS*0IV12BHD+3M0ANR-HF9RXAVPASN:39+5E6G$1LN2EB*HR0O WVR8OE3AA6W%CBT5G.ZU 03GKFX+EWOR SJ:VO DTI.HTXT97Q 2EP7WN3U:CV.BSQRK"

negative_variants = generate_negative_hc1_variants(valid_hc1, count=15)

for i, v in enumerate(negative_variants, 1):
    print(v)
    # print(f"{i}: {v[:80]}{'...' if len(v) > 80 else ''}")  # print first 80 chars
