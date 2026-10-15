import os
import re

emoji_pattern = re.compile(
    "["
    u"\U0001f300-\U0001f5ff"
    u"\U0001f900-\U0001f9ff"
    u"\U0001f600-\U0001f64f"
    u"\U0001f680-\U0001f6ff"
    u"\u2600-\u26ff"
    u"\u2700-\u27bf"
    u"\U0001f1e6-\U0001f1ff"
    "]+",
    flags=re.UNICODE
)

def remove_emojis_from_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Remove emojis
    cleaned = emoji_pattern.sub('', content)
    # Also clean up any extra spaces left behind (e.g., "word  " -> "word ")
    cleaned = cleaned.replace('  ', ' ')
    
    if cleaned != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(cleaned)
        print(f"Cleaned {filepath}")

ui_dir = 'app/src/main/java/com/example/ui'
for filename in os.listdir(ui_dir):
    if filename.endswith('.kt'):
        remove_emojis_from_file(os.path.join(ui_dir, filename))
print("Emoji stripping complete.")
