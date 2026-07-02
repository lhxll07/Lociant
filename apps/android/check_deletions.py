import os
root = r'C:\Users\Lhx\Documents\Programs\Lociant'
checks = [
    'campaign_presentation',
    'apps/android/acp',
    'apps/android/app/src/main/java/com/mnnode/app/scene',
    'apps/android/app/src/main/java/com/mnnode/app/runtime/TriggerEngine.kt',
    'apps/android/phone-tools/src/main/java/com/mnnode/app/server/GadgetbridgeSnapshotReader.kt',
    'scenes',
    'apps/android/app/src/main/java/com/mnnode/app/server/ChatRequestQueue.kt',
    'apps/android/core/src/main/java/com/mnnode/app/util/JsonUtils.kt',
]
for name in checks:
    exists = os.path.exists(os.path.join(root, name))
    status = 'DELETED' if not exists else 'EXISTS'
    print(f'  [{status}] {name}')
