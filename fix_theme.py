import os
import glob

base = r'E:\Project_Android\lingdongmanhua\app\src\main\kotlin\cn\android\adhub\ui\screens'

replacements = [
    ('AdHubTheme.primary', 'MaterialTheme.colorScheme.primary'),
    ('AdHubTheme.background', 'MaterialTheme.colorScheme.background'),
    ('AdHubTheme.surface', 'MaterialTheme.colorScheme.surface'),
    ('AdHubTheme.onBackground', 'MaterialTheme.colorScheme.onBackground'),
    ('AdHubTheme.onSurface', 'MaterialTheme.colorScheme.onSurface'),
    ('AdHubTheme.surfaceVariant', 'MaterialTheme.colorScheme.surfaceVariant'),
]

fixed = []
for filepath in glob.glob(os.path.join(base, '**', '*.kt'), recursive=True):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    new_content = content
    for old, new in replacements:
        new_content = new_content.replace(old, new)
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        fixed.append(os.path.relpath(filepath, base))

print(f'Fixed {len(fixed)} files:')
for f in fixed:
    print(f'  {f}')
