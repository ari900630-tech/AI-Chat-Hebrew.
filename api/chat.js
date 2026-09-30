export default async function handler(req, res) {
  if (req.method !== "POST") return res.status(405).json({ error: "Method not allowed" });

  const key = process.env.GROQ_API_KEY;
  if (!key) return res.status(500).json({ error: "Server API key is not configured" });

  try {
    const { message } = req.body || {};
    if (!message || typeof message !== "string") {
      return res.status(400).json({ error: "Message is required" });
    }

    const response = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        "Authorization": "Bearer " + key,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: "llama-3.1-8b-instant",
        messages: [{ role: "user", content: message }]
      })
    });

    const data = await response.json();
    if (!response.ok) return res.status(response.status).json({ error: "Groq request failed" });

    return res.status(200).json({
      reply: data.choices?.[0]?.message?.content || "לא התקבלה תשובה."
    });
  } catch {
    return res.status(500).json({ error: "Server error" });
  }
}