# fix missing sensorGroups function
import re

path = r'C:\Users\Lhx\Documents\Programs\Lociant\apps\android\phone-tools\src\main\java\com\mnnode\app\server\AndroidTools.kt'
with open(path, 'r', encoding='utf-8') as f:
    c = f.read()

insert = '''
    private fun sensorGroups(groups: JSONArray?): Set<String> {
        if (groups == null || groups.length() == 0) return SENSOR_GROUPS
        val parsed = mutableSetOf<String>()
        for (index in 0 until groups.length()) {
            val group = groups.optString(index).trim().lowercase()
            if (group == "all") return SENSOR_GROUPS
            if (group in SENSOR_GROUPS) parsed += group
        }
        return parsed.ifEmpty { SENSOR_GROUPS }
    }

'''

c = c.replace(
    '    private fun sensorSpecs(groups: Set<String>): List<SensorSpec> = buildList {',
    insert + '    private fun sensorSpecs(groups: Set<String>): List<SensorSpec> = buildList {'
)

# Also check the stray closing paren at line 625
c = re.sub(r'        \)\n    }\n\n    private class User', r'\n    private class User', c)

with open(path, 'w', encoding='utf-8') as f:
    f.write(c)
print('Fixed')
