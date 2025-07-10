# LifeDash 📱📊 — The Offline Life Dashboard + GPT Assistant

A powerful productivity-first Android app that visualizes your phone habits — from app usage to unlocks to screen time — entirely **offline** and **without any login**. LifeDash goes beyond just tracking — it reflects, motivates, and transforms your digital behavior with the help of an integrated GPT assistant.

---

## 📸 Screenshots

<table>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/3a69dcb0-d6e8-41e7-80c2-7b37ea9a4299" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/94037be6-984a-47f3-88ff-8af9c77f4173" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/cae20bf4-65c3-4506-a3c5-dd2c7888c96e" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/b3f218b7-35d4-467d-8300-cfeba9fa7a89" width="200"/></td>
  </tr>
  <tr>
    <td><img src="https://github.com/user-attachments/assets/e27d87b8-5ab8-4ba8-bb1e-d379ba5660e8" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/5b7f4896-f97b-43aa-9f5b-7a95eeca04e9" width="200"/></td>
    <td><img src="https://github.com/user-attachments/assets/664ba3aa-eb92-4d9b-8d12-ca42ccb6106e" width="200"/></td>
  </tr>
</table>

---

## ✨ Key Features

- 📊 **Daily Usage Dashboard**  
  View screen time, app sessions, unlocks, notification count, and on/off stats — all in one clean place.

- 🧠 **AI-Powered Summarization (GPT)**  
  Generate intelligent daily summaries based on your usage + actions. Understand *how your day really went* — not just the numbers.

- 📅 **Interactive Calendar View**  
  Pick any past date and view a highlight of your phone activity that day. Spot trends, spikes, or low-usage days.

- 🔐 **No Servers. No Login. No Cloud.**  
  Everything runs locally — analytics, summaries, and enforcement. Your data stays with you.

- 🎯 **Productivity Nudges & Commands**  
  Tell GPT to:
  - “Lock Instagram after 30 mins”
  - “Show most distracted hour”
  - “Give me detox tips”  
  ...and more.

- 📈 **8+ Insightful Charts**  
  Heatmaps, hourly line graphs, app rankings, notification timelines, and unlock spikes — visualized beautifully using Jetpack Compose.

- 🧱 **Lightweight & Secure**  
  App size under 10MB. Built with Kotlin, Jetpack Compose, and zero 3rd-party analytics.

---

## ⚙️ Tech Stack

| Layer | Tech |
|--|--|
| UI | Jetpack Compose |
| Logic | Kotlin, UsageStatsManager |
| Charts | MPAndroidChart |
| GPT | Local integration with on-device prompts |
| Offline Storage | Jetpack DataStore & in-memory caching |
| Permissions | Usage Access, Notification Listener |

---

## 🚀 Future Additions (Coming Soon)

- [ ] App blocking after set limits (auto enforced)
- [ ] Weekly summary reports
- [ ] Usage streak tracking & challenges
- [ ] Focus mode with positive nudges

---

## 📦 Getting Started

```bash
git clone https://github.com/NamanShah2005/LifeDash.git
open with Android Studio
