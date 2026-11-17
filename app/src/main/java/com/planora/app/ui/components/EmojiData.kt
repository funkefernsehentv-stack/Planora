package com.planora.app.ui.components

/** Umfangreiche Emoji-Auswahl nach Kategorien. Über das Textfeld kann zusätzlich jedes beliebige Emoji eingegeben werden. */
object EmojiData {
    val categories: List<Pair<String, List<String>>> = listOf(
        "Geburtstage" to "🎂 🎁 🎈 🎉 🎊 🥳 🍰 🧁 🕯️ 🎀 🪅 🍾 🥂 👑 🎶".split(" "),
        "Feiertage" to "🎄 🎅 🤶 ⛄ ❄️ 🎆 🎇 🧨 🐣 🐰 🥚 🎃 👻 🦇 🕸️ 💝 💘 🌹 🕎 🪔 🌙 ☪️ 🍀 🇩🇪".split(" "),
        "Reisen" to "✈️ 🧳 🏖️ 🏝️ 🗺️ 🧭 🏔️ ⛰️ 🏕️ 🚗 🚆 🚢 🛳️ 🗽 🗼 🏰 🌍 🌴 🎡 🛫 🛬 🏨 📸".split(" "),
        "Schule" to "📚 🎓 ✏️ 📝 📖 🏫 🎒 📐 📏 🧮 🔬 🧪 🖍️ 📓 🧑‍🏫 💯 🏆 📅".split(" "),
        "Sport" to "⚽ 🏀 🏈 ⚾ 🎾 🏐 🏓 🏸 🥊 🏋️ 🚴 🏃 🏊 🧘 ⛷️ 🏂 🤸 🥇 🏅 🎿 🛹 🧗".split(" "),
        "Freunde" to "👯 🤝 🫂 🍻 🥂 🍕 🎮 🎤 🎬 🍿 🛍️ 💬 😂 🤗 🙌 ✌️ 💃 🕺".split(" "),
        "Familie" to "👨‍👩‍👧 👨‍👩‍👦 👪 ❤️ 🏠 👶 🍼 👵 👴 🐶 🐱 🍽️ 🧸 💍 💒 👰 🤵".split(" "),
        "Arbeit" to "💼 💻 🖥️ 📊 📈 📉 🗂️ 📁 📎 🖊️ ☎️ 📞 📧 🗓️ ⏰ 🏢 🧾 💰 🤝 🎯".split(" "),
        "Gesundheit" to "💊 🩺 🏥 🦷 👓 🩹 💉 🧠 ❤️‍🩹 🥗 🍎 💧 😴 🧴 🧘‍♀️ 🚭".split(" "),
        "Freizeit" to "🎨 🎸 🎹 🎧 📷 🎲 🧩 🎳 🎣 🌱 🪴 📺 🎭 🎟️ 🎪 🍳 ☕ 🍷 🛋️ 🧶".split(" "),
        "Besondere Ereignisse" to "💍 💒 🎓 🏆 🥇 🎤 🎬 🎫 🎟️ 🌟 ⭐ ✨ 💫 🔥 🚀 🎯 📣 🪩 🏁".split(" "),
        "Natur & Wetter" to "☀️ 🌤️ ⛅ 🌧️ ⛈️ 🌈 🌸 🌻 🌷 🍁 🍂 🌊 🌙 ⭐ 🌲 🦋 🐝 🐞".split(" "),
        "Essen" to "🍕 🍔 🍣 🍝 🥐 🥨 🍓 🍉 🍩 🍪 🍫 🍦 🥑 🌮 🥞 🍜 🥤 🧃".split(" "),
        "Symbole" to "⭐ ❗ ❓ ✅ ❌ ⚠️ 💡 📌 🔔 🏷️ 🔖 💎 🧿 ♻️ ⏳ ⌛ 🕐 📍 🔑 💖 💜 💙 💚 💛 🧡".split(" "),
    )
}
