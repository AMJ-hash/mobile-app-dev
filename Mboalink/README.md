 MboaLink

Connecting Cameroon's skilled workers with clients — one chat at a time.

MboaLink is a web app that lets clients find, verify, and book trusted skilled workers (electricians, plumbers, tailors, maids, cleaners, masons, and more) across Cameroon. Workers create a verified profile, upload their previous work, and communicate with clients through a real-time chat before agreeing on a booking.

---

 Features

-  Worker profiles** — photo, bio, trade, price range, city, trust score
- CNI verification** — workers upload a photo of their National ID card (only valid landscape-format CNI photos are accepted)
- Email verification** — every new worker account is verified by email before going live
- Real-time chat** — clients and workers chat instantly using WebSockets (Socket.IO) with typing indicators and online status
- Booking confirmation** — agree on price and payment method inside the chat
- Portfolio** — workers upload photos of previous completed jobs for clients to see
- Reviews & badges** — clients rate workers and award badges after a job
- AI matching** — finds the best worker based on rating, availability, city, and trust score
- Emergency mode** — instantly finds the nearest available worker
  3 languages** — English, French, Cameroonian Pidgin

---

 Tech Stack

| Layer | Technology |
|---|---|
| Backend | Python 3 + Flask |
| Real-time chat | Flask-SocketIO (WebSockets) |
| Email | Resend API |
| Image validation | Pillow (PIL) |
| Frontend | HTML5 + CSS3 + Vanilla JS + Socket.IO client |
| Templating | Jinja2 |
| Tunnelling (dev) | ngrok |

---

Getting Started

### 1. Install dependencies

```bash
pip install flask flask-socketio requests pillow
```

### 2. Get your free Resend API key

1. Go to [https://resend.com](https://resend.com) and create a free account
2. Go to **API Keys** → **Create API Key**
3. Copy the key — it starts with `re_`

> **Free plan:** 3,000 emails/month, 100/day — more than enough

### 3. Set up ngrok (so your app works on other phones)

1. Download from [https://ngrok.com](https://ngrok.com) — free account
2. Run: `ngrok config add-authtoken YOUR_TOKEN`
3. In a second terminal: `ngrok http 5000`
4. Copy the `https://xxxx.ngrok-free.app` URL it gives you

 4. Run the app

**Mac / Linux:**
```bash
RESEND_API_KEY=re_your_key APP_URL=https://xxxx.ngrok-free.app python app.py
```

Windows (Command Prompt):
cmd
set RESEND_API_KEY=re_your_key
set APP_URL=https://xxxx.ngrok-free.app
python app.py
```

Then open `http://localhost:5000` on your machine, or the ngrok URL on any other device.

---

 Environment Variables

| Variable | Required | Description |
|---|---|---|
| `RESEND_API_KEY` | Yes (for email) | Your Resend API key — get free at resend.com |
| `APP_URL` | Yes (for email links) | The public URL of your app (ngrok URL or deployed domain) |
| `RESEND_FROM` | Optional | From address for emails. Default: `MboaLink <onboarding@resend.dev>` |

---

## 📧 Email — Why Only One Email Works Right Now

MboaLink sends verification emails through **Resend**. On Resend's free plan, you can only send emails **from** a verified domain. Until a custom domain is fully verified, Resend restricts delivery to only the account owner's email address.

**Current status:** The MboaLink domain has been purchased and DNS records have been added. Resend domain verification is in progress (takes 24–48 hours). Once verified, **all users** will receive verification emails normally.

**In the meantime:** The verify page has a manual activation button so workers can still activate their account during testing.

---

 ngrok — Making the App Work on Other Phones

When running locally, your app is at `http://localhost:5000` — this only works on your own machine. ngrok creates a public URL so anyone can open your app on their phone.

```
Your laptop (localhost:5000)  ←→  ngrok tunnel  ←→  Public URL (https://xxxx.ngrok-free.app)
```



1. Client opens a worker profile and clicks **Book & Chat**
2. A private chat room is created between client and worker
3. Both users connect via **WebSocket** — a permanent live connection
4. When one person sends a message, it appears instantly on the other's screen — no refresh needed
5. When they agree on terms, they confirm the booking with price and payment method
6. A WhatsApp link is provided for further coordination

---

 Project Structure

```
MboaLink_v5/
├── app.py                    ← All routes, logic, Socket.IO events
├── requirements.txt          ← Python dependencies
├── templates/
│   ├── base.html             ← Master layout (navbar, footer)
│   ├── login.html            ← Login page
│   ├── home.html             ← Client dashboard
│   ├── workers.html          ← Browse workers
│   ├── worker_detail.html    ← Single worker profile + portfolio
│   ├── worker_dashboard.html ← Worker home page
│   ├── worker_profile.html   ← Edit profile + add portfolio
│   ├── chat.html             ← Real-time chat room
│   ├── my_chats.html         ← All conversations
│   ├── jobs.html             ← Job listings
│   ├── post_job.html         ← Post a job
│   ├── leave_review.html     ← Review + badge form
│   └── verify_worker.html    ← Email verification page
└── static/
    ├── css/style.css         ← All styling
    ├── js/main.js            ← Frontend JS
    └── uploads/              ← Profile photos, CNI photos, portfolio images
```

---

 User Roles

Client
- Browse and search workers by trade, city, name
- View worker profiles, portfolios, reviews
- Book a worker by opening a chat
- Post jobs for workers to apply to
- Leave reviews and award badges after work is do Worker
- Register with CNI photo upload + email verification
- Dashboard showing notifications, open jobs, portfolio, reviews
- Apply to posted jobs
- Chat with clients in real time
- Edit profile: bio, price, availability status, profile photo
- Add portfolio items (photos of previous work)

---

Current Limitations

- **Email limited** — only sends to domain owner's email until Resend domain verification completes.
- **ngrok URL changes** — on the free plan, the URL is different each time you restart ngrok.

---

 Roadmap

- [ ] Add SQLite/PostgreSQL database for persistent data
- [ ] Full user authentication with passwords
- [ ] Fapshi payment integration (70% upfront, 30% escrow)
- [ ] Admin panel to review and approve CNI photos
- [ ] Android app (WebView wrapper)
- [ ] SMS fallback for users without email
- [ ] Push notifications

---

## Supported Cities

Yaounde · Douala · Bafoussam · Bamenda · Garoua · Maroua · Ngaoundere · Bertoua · Kribi · Limbe

 Supported Trades

Electrician · Plumber · Painter · Mason · Carpenter · Tiler · Welder · Tailor · Cleaner · Maid · Other

---

 License

Built for Cameroon. All rights reserved © MboaLink 2026.
