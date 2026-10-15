import re

with open("app/src/main/java/com/example/ui/AppUI.kt", "r") as f:
    content = f.read()

# Remove MovementRecord from AppUI.kt
content = re.sub(r'    data class MovementRecord\(\n(?:.*\n){10}    \)', '', content)

with open("app/src/main/java/com/example/ui/AppUI.kt", "w") as f:
    f.write(content)

with open("app/src/main/java/com/example/data/Models.kt", "a") as f:
    f.write("\n\ndata class MovementRecord(\n"
            "    val id: String,\n"
            "    val date: Long,\n"
            "    val type: String,\n"
            "    val isIncoming: Boolean,\n"
            "    val quantity: Int,\n"
            "    val price: Double,\n"
            "    val total: Double,\n"
            "    val partyName: String,\n"
            "    val reference: String,\n"
            "    val invoiceId: Int? = null\n"
            ")\n")
