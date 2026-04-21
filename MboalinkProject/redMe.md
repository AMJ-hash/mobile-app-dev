# MboaLink

A web app that connects clients with skilled workers (electricians, plumbers, tailors, maids, cleaners, masons and more) across Cameroon. Clients browse verified worker profiles, open a real-time chat to discuss the job, and confirm a booking — all in one place.

---

## Setup

Install dependencies:
```bash
pip install flask flask-socketio requests pillow
```

Run the app:
```bash
# Mac / Linux
RESEND_API_KEY=re_your_key APP_URL=https://your-ngrok-url.ngrok-free.app python app.py

# Windows
set RESEND_API_KEY=re_your_key
set APP_URL=https://your-ngrok-url.ngrok-free.app
python app.py
```

Then open http://localhost:5000.

---

## Environment Variables

| Variable | Description |
|---|---|
| RESEND_API_KEY | Email API key from resend.com (free) |
| APP_URL | Public URL of your app (ngrok URL or deployed domain) |

---

## Email Verification

Worker accounts are verified by email using the Resend API. On the free plan, Resend only delivers emails to the account owner's address until a custom domain is verified. The MboaLink domain has been purchased and is currently being verified — once that is done, all users will receive emails normally. In the meantime, the verify page includes a manual activation button for testing.

---

## ngrok

Running locally means the app is only accessible on your machine at localhost:5000. ngrok creates a public URL so the app works on any phone or device. Get it free at ngrok.com, then run ngrok http 5000 in a second terminal and use the URL it gives you as APP_URL.

---

## Tech Stack

- Python 3, Flask, Flask-SocketIO
- Resend (email), Pillow (CNI image validation)
- Jinja2 templates, vanilla JS, Socket.IO client

---

## Current Limitations

- No database — all data is stored in memory and is lost when the server restarts
- No password login for clients
- Email currently only reaches the account owner until domain verification completes
