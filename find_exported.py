import os
import xml.etree.ElementTree as ET

manifests = []
for root, dirs, files in os.walk('.'):
    if 'build' in dirs:
        dirs.remove('build')
    for f in files:
        if f == 'AndroidManifest.xml':
            manifests.append(os.path.join(root, f))

for path in manifests:
    try:
        tree = ET.parse(path)
        root = tree.getroot()
        for comp_type in ['activity', 'service', 'receiver', 'provider']:
            for comp in root.findall(f'.//{{*}}{comp_type}'):
                has_intent = False
                for child in comp:
                    if 'intent-filter' in child.tag:
                        has_intent = True
                
                has_exported = '{http://schemas.android.com/apk/res/android}exported' in comp.attrib
                if has_intent and not has_exported:
                    name = comp.attrib.get('{http://schemas.android.com/apk/res/android}name')
                    print(f'{path} -> {comp_type} {name}')
    except Exception as e:
        print(f'Error parsing {path}: {e}')
