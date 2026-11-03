import os
import re

manifests = []
for root, dirs, files in os.walk('.'):
    if 'build' in dirs:
        dirs.remove('build')
    for f in files:
        if f == 'AndroidManifest.xml':
            manifests.append(os.path.join(root, f))

# Find components with intent-filter but no android:exported
for path in manifests:
    try:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Regex to find <activity, <service, <receiver, <provider blocks
        # that contain <intent-filter> but don't have android:exported
        
        # We will split the content by <activity, <service, etc and then check if they have exported.
        # But a regex that finds the component open tag and injects exported="true" is better.
        
        # A simpler approach: find all occurrences of <service, <activity, <receiver, <provider
        # find the end of the tag (>). check if between <service and > there is android:exported.
        # If not, check if between > and </service> there is <intent-filter>.
        # If so, inject android:exported="true" into the open tag.

        # Because XML can be tricky, let's use a simpler string matching.
        # Let's just run this python script to do it.

        import xml.etree.ElementTree as ET
        tree = ET.parse(path)
        root = tree.getroot()
        to_fix = []
        for comp_type in ['activity', 'service', 'receiver', 'provider']:
            for comp in root.findall(f'.//{{*}}{comp_type}'):
                has_intent = False
                for child in comp:
                    if 'intent-filter' in child.tag:
                        has_intent = True
                
                has_exported = '{http://schemas.android.com/apk/res/android}exported' in comp.attrib
                if has_intent and not has_exported:
                    name = comp.attrib.get('{http://schemas.android.com/apk/res/android}name')
                    to_fix.append((comp_type, name))
        
        if to_fix:
            print(f'Fixing {path}')
            new_content = content
            for comp_type, name in to_fix:
                # Find the tag <comp_type ... android:name="name" ... >
                # We can do this with a regex:
                # <comp_type\s+([^>]*?)android:name=["']([^"']*)name([^"']*)["']([^>]*?)>
                # It's easier: just find android:name="name" inside the file and add android:exported="true" on the same line or next line.
                
                # Let's search for android:name="name"
                # Since name might be fully qualified or relative, we use exact match.
                name_attr = f'android:name="{name}"'
                if name_attr in new_content:
                    replacement = f'android:exported="true"\n            {name_attr}'
                    new_content = new_content.replace(name_attr, replacement)
            
            with open(path, 'w', encoding='utf-8') as f:
                f.write(new_content)

    except Exception as e:
        print(f'Error fixing {path}: {e}')
