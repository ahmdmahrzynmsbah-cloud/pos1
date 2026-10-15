import urllib.request
import json

url = "https://restcountries.com/v3.1/all"
req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
try:
    with urllib.request.urlopen(req) as response:
        data = json.loads(response.read().decode())
except Exception as e:
    print(f"Failed to fetch data: {e}")
    exit(1)

out = "package com.example.data\n\n"
out += "data class CountryCode(val name: String, val code: String, val flag: String)\n\n"
out += "val allCountryCodes = listOf(\n"

unique_countries = []
for country in data:
    try:
        if 'name' not in country: continue
        name_data = country.get('name', {})
        common_name = name_data.get('common', '')
        
        # Try getting arabic translation
        ara_name = country.get('translations', {}).get('ara', {}).get('common', common_name)
        name = ara_name if ara_name else common_name
        
        idd = country.get('idd', {})
        root = idd.get('root', '')
        suffixes = idd.get('suffixes', [])
        
        if not root: continue
        
        if len(suffixes) == 1:
            code = root + suffixes[0]
        else:
            code = root
            
        flag = country.get('flag', '')
        if not flag: continue
        
        unique_countries.append((name, code, flag))
    except Exception as e:
        print(f"Exception on {country.get('name')}: {e}")

# Deduplicate
seen = set()
dedup = []
for name, code, flag in unique_countries:
    if (name, code) not in seen:
        seen.add((name, code))
        dedup.append((name, code, flag))

for name, code, flag in dedup:
    out += f'    CountryCode("{name}", "{code}", "{flag}"),\n'

out += ").sortedBy { it.name }\n"

with open("app/src/main/java/com/example/data/CountryCodes.kt", "w") as f:
    f.write(out)

print(f"Generated CountryCodes.kt with {len(dedup)} countries")
