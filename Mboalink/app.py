"""
MboaLink v5
- Workers: dashboard, edit profile, portfolio, browse & apply to jobs
- Clients: browse workers, post jobs, book via CHAT
- Booking = open a chat room between client & worker
  (discuss details + agree on payment method in chat)
- Real email verification via Resend API
- CNI photo validation (Cameroon ID landscape check via Pillow)
- Profile pictures actually stored and shown

Setup:
  pip install flask requests pillow

Get free Resend key at https://resend.com then:
  RESEND_API_KEY=re_xxxx APP_URL=http://localhost:5000 python app.py
"""

from flask import (Flask, render_template, request, redirect,
                   url_for, session, jsonify, flash)
import uuid, os, datetime, random, string

app = Flask(__name__)
app.secret_key = "mboalink_v5_2025"
app.config["SESSION_COOKIE_SAMESITE"] = "Lax"
app.config["PERMANENT_SESSION_LIFETIME"] = datetime.timedelta(days=7)

@app.before_request
def make_session_permanent():
    session.permanent = True
UPLOAD_FOLDER = os.path.join("static", "uploads")
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

# ── CONFIG ────────────────────────────────────────────────────────────────────
RESEND_API_KEY = os.environ.get("RESEND_API_KEY", "")
RESEND_FROM    = os.environ.get("RESEND_FROM", "MboaLink <onboarding@resend.dev>")
APP_URL        = os.environ.get("APP_URL", "http://localhost:5000")

# ── TRADES / CITIES ───────────────────────────────────────────────────────────
TRADES_EN  = ["Electrician","Plumber","Painter","Mason","Carpenter","Tiler","Welder","Tailor","Cleaner","Maid","Other"]
TRADES_FR  = ["Électricien","Plombier","Peintre","Maçon","Menuisier","Carreleur","Soudeur","Tailleur","Agent de nettoyage","Femme de ménage","Autre"]
TRADES     = {"en": TRADES_EN, "fr": TRADES_FR, "pid": TRADES_EN}
TRADE_ICONS = {
    "Electrician":"⚡","Plumber":"🔧","Painter":"🎨","Mason":"🧱","Carpenter":"🪵",
    "Tiler":"🏠","Welder":"🔩","Tailor":"🧵","Cleaner":"🧹","Maid":"🏡","Other":"🛠",
    "Électricien":"⚡","Plombier":"🔧","Peintre":"🎨","Maçon":"🧱","Menuisier":"🪵",
    "Carreleur":"🏠","Soudeur":"🔩","Tailleur":"🧵","Agent de nettoyage":"🧹",
    "Femme de ménage":"🏡","Autre":"🛠",
}
FR_TO_EN = dict(zip(TRADES_FR, TRADES_EN))
EN_TO_FR = dict(zip(TRADES_EN, TRADES_FR))

CITIES = ["Yaounde","Douala","Bafoussam","Bamenda","Garoua","Maroua","Ngaoundere","Bertoua","Kribi","Limbe"]
PRICE_GUIDE = {
    "Electrician":"8,000–60,000 XAF","Plumber":"5,000–30,000 XAF","Painter":"5,000–100,000 XAF",
    "Mason":"15,000–250,000 XAF","Carpenter":"10,000–150,000 XAF","Tiler":"8,000–50,000 XAF",
    "Welder":"10,000–80,000 XAF","Tailor":"3,000–50,000 XAF","Cleaner":"5,000–30,000 XAF",
    "Maid":"30,000–80,000 XAF/month","Other":"5,000–50,000 XAF",
    "Électricien":"8,000–60,000 XAF","Plombier":"5,000–30,000 XAF","Peintre":"5,000–100,000 XAF",
    "Maçon":"15,000–250,000 XAF","Menuisier":"10,000–150,000 XAF","Carreleur":"8,000–50,000 XAF",
    "Soudeur":"10,000–80,000 XAF","Tailleur":"3,000–50,000 XAF",
    "Agent de nettoyage":"5,000–30,000 XAF","Femme de ménage":"30,000–80,000 XAF/mois","Autre":"5,000–50,000 XAF",
}
BADGE_OPTIONS = [
    "Very Professional 🏆","On Time ⏰","Clean Work ✨","Friendly 😊",
    "Great Value 💰","Highly Recommended ⭐","Fast Service ⚡","Honest 🤝",
]

# ── IN-MEMORY DATA ────────────────────────────────────────────────────────────
WORKERS       = []   # worker profile dicts
JOBS          = []   # job post dicts
REVIEWS       = []   # review dicts
CHATS         = {}   # chat_id -> {worker_id, client_email, worker_name, client_name, job_id?, messages:[]}
notifications = {}   # email -> [notif dicts]
verify_tokens = {}   # token -> registration data dict
portfolio_db  = {}   # worker_id -> [item dicts]

# Demo workers
WORKERS = [
  {"id":"w1","name":"Jean-Paul Mbida","trade_en":"Electrician","trade_fr":"Électricien",
   "emoji":"⚡","city":"Yaounde","phone":"+237 677 123 456","email":"jpmbida@demo.cm",
   "cni_verified":True,"email_verified":True,"rating":4.9,"jobs":67,"ratings":61,
   "status":"available","price_min":10000,"price_max":50000,
   "bio_en":"Electrician with 9 years of experience. Home wiring, fast repairs. 7 days a week.",
   "bio_fr":"Électricien avec 9 ans d'expérience. Installation domestique, dépannage rapide.",
   "badges":["Top Rated 🏆","Always On Time ⏰"],"pic":"","cni_pic":"",
   "cert_en":"Certified Electrician","cert_fr":"Électricien Certifié"},
  {"id":"w2","name":"Emmanuel Ngono","trade_en":"Plumber","trade_fr":"Plombier",
   "emoji":"🔧","city":"Douala","phone":"+237 699 234 567","email":"engono@demo.cm",
   "cni_verified":True,"email_verified":True,"rating":4.6,"jobs":41,"ratings":38,
   "status":"available","price_min":5000,"price_max":25000,
   "bio_en":"Professional plumber. Leak repairs, pipe installation, water heaters.",
   "bio_fr":"Plombier sérieux. Fuite d'eau, installation sanitaire, chauffe-eau.",
   "badges":["Reliable 🤝"],"pic":"","cni_pic":"","cert_en":"Certified Plumber","cert_fr":"Plombier Certifié"},
  {"id":"w3","name":"Christelle Fomba","trade_en":"Tailor","trade_fr":"Tailleur",
   "emoji":"🧵","city":"Yaounde","phone":"+237 655 345 678","email":"cfomba@demo.cm",
   "cni_verified":True,"email_verified":True,"rating":4.2,"jobs":18,"ratings":15,
   "status":"available","price_min":3000,"price_max":40000,
   "bio_en":"Expert tailor. Traditional and modern clothing, alterations, custom designs.",
   "bio_fr":"Tailleuse experte. Tenues traditionnelles et modernes, retouches sur mesure.",
   "badges":["Rising Star ⭐"],"pic":"","cni_pic":"","cert_en":"","cert_fr":""},
  {"id":"w4","name":"Patrice Elong","trade_en":"Mason","trade_fr":"Maçon",
   "emoji":"🧱","city":"Bafoussam","phone":"+237 677 456 789","email":"pelong@demo.cm",
   "cni_verified":True,"email_verified":True,"rating":4.8,"jobs":58,"ratings":52,
   "status":"available","price_min":20000,"price_max":200000,
   "bio_en":"General masonry, tiling, concrete work. Quality materials on request.",
   "bio_fr":"Maçonnerie générale, carrelage, chape béton. Matériaux de qualité.",
   "badges":["Gold Level ⭐","ID Verified ✅"],"pic":"","cni_pic":"",
   "cert_en":"Expert Mason","cert_fr":"Maçon Expert"},
  {"id":"w5","name":"Aïcha Nkemdirim","trade_en":"Maid","trade_fr":"Femme de ménage",
   "emoji":"🏡","city":"Douala","phone":"+237 699 567 890","email":"ankemdirim@demo.cm",
   "cni_verified":True,"email_verified":True,"rating":4.7,"jobs":33,"ratings":30,
   "status":"available","price_min":30000,"price_max":80000,
   "bio_en":"Experienced household maid. Cooking, cleaning, childcare. References available.",
   "bio_fr":"Femme de ménage expérimentée. Cuisine, ménage, garde d'enfants.",
   "badges":["Trustworthy 🤝","Family Friendly 👨‍👩‍👧"],"pic":"","cni_pic":"","cert_en":"","cert_fr":""},
  {"id":"w6","name":"Boris Kamdem","trade_en":"Cleaner","trade_fr":"Agent de nettoyage",
   "emoji":"🧹","city":"Yaounde","phone":"+237 677 900 111","email":"bkamdem@demo.cm",
   "cni_verified":True,"email_verified":True,"rating":4.5,"jobs":22,"ratings":20,
   "status":"available","price_min":5000,"price_max":25000,
   "bio_en":"Professional cleaning services for homes, offices and events.",
   "bio_fr":"Services de nettoyage professionnels pour domiciles, bureaux et événements.",
   "badges":["Clean Work ✨"],"pic":"","cni_pic":"","cert_en":"","cert_fr":""},
]
REVIEWS = [
  {"wid":"w1","from_name":"Alphonse N.","rating":5,"comment":"Excellent, very professional. Fixed everything in 2 hours!","date":"12/01/2024"},
  {"wid":"w1","from_name":"Bernadette K.","rating":4,"comment":"Good work, result perfect.","date":"05/02/2024"},
  {"wid":"w2","from_name":"Clement M.","rating":5,"comment":"Leak fixed in 30 min! Super fast.","date":"18/03/2024"},
  {"wid":"w4","from_name":"Denise T.","rating":4,"comment":"Serious mason, quality work.","date":"22/03/2024"},
]

# ── HELPERS ───────────────────────────────────────────────────────────────────
# ── TRANSLATIONS (module level — built once, not on every call) ───────────────
_TR_EN = {
    "nav_home":"Home","nav_workers":"Find Workers","nav_jobs":"Jobs",
    "nav_emergency":"Emergency","nav_logout":"Logout","nav_profile":"Profile",
    "nav_dashboard":"Dashboard","nav_my_chats":"My Chats",
    "hi":"Hello","tagline":"Trusted skilled workers, at your fingertips",
    "city_badge":"Your city","emergency_btn":"🚨 EMERGENCY — Find Worker Now",
    "workers_label":"Workers","jobs_label":"Open Jobs","cities_label":"Cities",
    "top_workers":"Top Rated Workers","quick_actions":"Quick Actions",
    "browse_workers":"Browse Workers","browse_jobs":"Browse Jobs",
    "post_job":"Post a Job","emergency":"Emergency",
    "search_placeholder":"Search by name, trade or city...",
    "all_trades":"All Trades","all_cities":"All Cities","search_btn":"Search",
    "ai_heading":"AI Matching","ai_btn":"Find Best Match",
    "results_label":"result(s) found","jobs_done":"jobs done",
    "trust_label":"Trust","reviews_label":"reviews","verified":"✅ ID Verified",
    "available":"Available","busy":"Busy","offline":"Offline",
    "call_btn":"📞 Call","whatsapp_btn":"💬 WhatsApp",
    "book_btn":"💬 Book & Chat","about_label":"About","badges_label":"Badges",
    "client_reviews":"Client Reviews","no_reviews":"No reviews yet.",
    "price_range":"Price Range","trust_score":"Trust Score",
    "loyalty_label":"Level","experience":"Experience",
    "identity":"Identity","availability":"Availability",
    "jobs_title":"Job Listings","post_btn":"+ Post a Job",
    "job_title_label":"Job Title *","job_desc_label":"Description *",
    "trade_label":"Trade","city_label":"City","address_label":"Address",
    "budget_label":"Budget (XAF) *","urgent_label":"🚨 Urgent job",
    "group_label":"👥 Group job","workers_needed":"Workers needed",
    "publish_btn":"Publish Job","job_success":"Job published!",
    "price_hint":"Estimated price for",
    "profile_heading":"My Profile","login_heading":"Welcome to MboaLink",
    "fullname":"Full Name","phone_label":"Phone Number",
    "iam":"I am a...","client_role":"Client","worker_role":"Worker",
    "login_btn":"Login / Create Account",
    "status_open":"Open","status_inprog":"In Progress","status_done":"Completed",
    "contact_client":"Contact Client","pay_btn":"Book & Chat",
    "posted_by":"Posted by","budget_display":"Budget","address_display":"Address",
    "status_display":"Status","desc_display":"Description",
    "no_workers":"No workers found.","lang_label":"Language",
    "worker_reg_title":"Worker Registration",
    "trade_select":"Your Trade *","bio_label":"Describe yourself to clients *",
    "price_min_label":"Min. Price (XAF) *","price_max_label":"Max. Price (XAF) *",
    "profile_pic":"Profile Photo *",
    "complete_job_btn":"✅ Mark Job as COMPLETED",
    "messages_title":"Chat","send_message":"Send",
    "message_placeholder":"Type a message...",
    "no_messages":"No messages yet. Start the conversation!",
    "notif_title":"Notifications","group_info":"workers needed",
    "escrow_active":"🔒 Escrow active","group_badge":"Group","no_messages_notif":"No notifications.",
}

_TR_FR = {
    "nav_home":"Accueil","nav_workers":"Artisans","nav_jobs":"Offres",
    "nav_emergency":"Urgence","nav_logout":"Déconnexion","nav_profile":"Profil",
    "nav_dashboard":"Tableau de bord","nav_my_chats":"Mes Chats",
    "hi":"Bonjour","tagline":"L'artisan de confiance, à portée de main",
    "city_badge":"Votre ville","emergency_btn":"🚨 URGENCE — Trouver un artisan",
    "workers_label":"Artisans","jobs_label":"Offres ouvertes","cities_label":"Villes",
    "top_workers":"Meilleurs artisans","quick_actions":"Actions rapides",
    "browse_workers":"Voir les artisans","browse_jobs":"Voir les offres",
    "post_job":"Publier une offre","emergency":"Urgence",
    "search_placeholder":"Chercher par nom, métier ou ville...",
    "all_trades":"Tous les métiers","all_cities":"Toutes les villes","search_btn":"Rechercher",
    "ai_heading":"Matching IA","ai_btn":"Trouver le meilleur",
    "results_label":"résultat(s) trouvé(s)","jobs_done":"chantiers",
    "trust_label":"Confiance","reviews_label":"avis","verified":"✅ ID Vérifié",
    "available":"Disponible","busy":"Occupé","offline":"Hors ligne",
    "call_btn":"📞 Appeler","whatsapp_btn":"💬 WhatsApp",
    "book_btn":"💬 Réserver & Discuter","about_label":"À propos","badges_label":"Badges",
    "client_reviews":"Avis clients","no_reviews":"Aucun avis pour le moment.",
    "price_range":"Fourchette de prix","trust_score":"Score de confiance",
    "loyalty_label":"Niveau","experience":"Expérience",
    "identity":"Identité","availability":"Disponibilité",
    "jobs_title":"Offres de travail","post_btn":"+ Publier une offre",
    "job_title_label":"Titre du chantier *","job_desc_label":"Description *",
    "trade_label":"Métier","city_label":"Ville","address_label":"Adresse",
    "budget_label":"Budget (XAF) *","urgent_label":"🚨 Urgent",
    "group_label":"👥 Chantier en groupe","workers_needed":"Nombre d'artisans",
    "publish_btn":"Publier l'offre","job_success":"Offre publiée avec succès !",
    "price_hint":"Prix estimé pour",
    "profile_heading":"Mon Profil","login_heading":"Bienvenue sur MboaLink",
    "fullname":"Nom complet","phone_label":"Numéro de téléphone",
    "iam":"Je suis...","client_role":"Client","worker_role":"Artisan",
    "login_btn":"Se connecter / Créer un compte",
    "status_open":"Disponible","status_inprog":"En cours","status_done":"Terminé",
    "contact_client":"Contacter le client","pay_btn":"Réserver & Discuter",
    "posted_by":"Publié par","budget_display":"Budget","address_display":"Adresse",
    "status_display":"Statut","desc_display":"Description",
    "no_workers":"Aucun artisan trouvé.","lang_label":"Langue",
    "worker_reg_title":"Inscription Artisan",
    "trade_select":"Votre métier *","bio_label":"Présentez-vous aux clients *",
    "price_min_label":"Prix min. (XAF) *","price_max_label":"Prix max. (XAF) *",
    "profile_pic":"Photo de profil *",
    "complete_job_btn":"✅ Marquer comme TERMINÉ",
    "messages_title":"Chat","send_message":"Envoyer",
    "message_placeholder":"Tapez un message...",
    "no_messages":"Aucun message. Lancez la conversation !",
    "notif_title":"Notifications","group_info":"artisans requis",
    "escrow_active":"🔒 Paiement sécurisé","group_badge":"Groupe","no_messages_notif":"Aucune notification.",
}

_TR_PID = {
    "nav_home":"Home","nav_workers":"Find Worker","nav_jobs":"Jobs",
    "nav_emergency":"Emergency","nav_logout":"Comot","nav_profile":"My Page",
    "nav_dashboard":"Dashboard","nav_my_chats":"My Chats",
    "hi":"How now","tagline":"Find good worker wey you fit trust",
    "city_badge":"Your town","emergency_btn":"🚨 EMERGENCY — Find Worker Quick Quick",
    "workers_label":"Workers","jobs_label":"Jobs Wey Dey Open","cities_label":"Towns",
    "top_workers":"Best Workers","quick_actions":"Do Something Fast",
    "browse_workers":"See Workers","browse_jobs":"See Jobs",
    "post_job":"Post Job","emergency":"Emergency",
    "search_placeholder":"Search name, work or town...",
    "all_trades":"All kain work","all_cities":"All towns","search_btn":"Search am",
    "ai_heading":"AI Go Find Worker","ai_btn":"Make AI Match Me",
    "results_label":"worker(s) dem find","jobs_done":"jobs don do",
    "trust_label":"Trust level","reviews_label":"reviews","verified":"✅ ID Don Check",
    "available":"E Dey Free","busy":"E Dey Busy","offline":"E No Dey",
    "call_btn":"📞 Call Am","whatsapp_btn":"💬 WhatsApp Am",
    "book_btn":"💬 Book & Chat","about_label":"About Am","badges_label":"Badges",
    "client_reviews":"Wetin People Talk","no_reviews":"Nobody don talk anything yet.",
    "price_range":"How Much E Go Cost","trust_score":"Trust Level",
    "loyalty_label":"Level","experience":"Experience",
    "identity":"ID Check","availability":"E Dey?",
    "jobs_title":"Jobs Wey Dey","post_btn":"+ Post Your Job",
    "job_title_label":"Wetin You Want Do *","job_desc_label":"Explain Am Well *",
    "trade_label":"Wetin Kain Work","city_label":"Your Town","address_label":"Exact Place",
    "budget_label":"How Much You Go Pay (XAF) *","urgent_label":"🚨 E Dey Urgent",
    "group_label":"👥 Na Team Work","workers_needed":"How Many Workers",
    "publish_btn":"Post The Job","job_success":"Job don post!",
    "price_hint":"How much e go cost for",
    "profile_heading":"My Tings","login_heading":"Enter MboaLink",
    "fullname":"Your Full Name","phone_label":"Your Phone Number",
    "iam":"Na Who You Be...","client_role":"Customer","worker_role":"Worker",
    "login_btn":"Enter / Make Account",
    "status_open":"E Dey Open","status_inprog":"E Dey Go","status_done":"E Don Finish",
    "contact_client":"Call The Person","pay_btn":"Book & Chat",
    "posted_by":"Who Post Am","budget_display":"How Much","address_display":"Place",
    "status_display":"Status","desc_display":"Explanation",
    "no_workers":"No worker dem find.","lang_label":"Language",
    "worker_reg_title":"Worker Registration",
    "trade_select":"Wetin Kain Work You Do *","bio_label":"Tell people about yourself *",
    "price_min_label":"Minimum price (XAF) *","price_max_label":"Maximum price (XAF) *",
    "profile_pic":"Profile Photo *",
    "complete_job_btn":"✅ Mark Work as FINISHED",
    "messages_title":"Chat","send_message":"Send Am",
    "message_placeholder":"Type your message...",
    "no_messages":"No message yet. Start talk!",
    "notif_title":"Notifications","group_info":"workers dem need",
    "escrow_active":"🔒 Money Safe","group_badge":"Team Work","no_messages_notif":"No notification.",
}

# Build final translation dict — FR missing keys fall back to EN (not raw key)
_TRANSLATIONS = {
    "en":  _TR_EN,
    "fr":  {**_TR_EN, **_TR_FR},   # EN base + FR overrides = nothing ever missing
    "pid": {**_TR_EN, **_TR_PID},  # EN base + PID overrides
}

def lang():
    return session.get("lang", "en")

def tr(key):
    """Look up translation for current language. Always falls back to English."""
    return _TRANSLATIONS.get(lang(), _TR_EN).get(key, _TR_EN.get(key, key))

def add_notif(email, text):
    if email:
        notifications.setdefault(email, []).insert(0, {
            "text": text,
            "time": datetime.datetime.now().strftime("%H:%M"),
        })

def loyalty(n):
    if n >= 50: return ("Gold ⭐", "#DAA520")
    if n >= 20: return ("Silver 🥈", "#999")
    if n >= 5:  return ("Bronze", "#CD7F32")
    return ("New", "#888")

def trust_score(w):
    s = (30 if w.get("cni_verified") else 0) + (20 if w.get("email_verified") else 0)
    s += min(w.get("jobs", 0), 30) + int(w.get("rating", 0) * 4)
    return min(s, 100)

def enrich_w(w):
    if not w: return {}
    l = lang(); w = dict(w)
    w["trade"]        = w.get(f"trade_{l}", w.get("trade_en", ""))
    w["bio"]          = w.get(f"bio_{l}",   w.get("bio_en", ""))
    w["cert"]         = w.get(f"cert_{l}",  w.get("cert_en", ""))
    w["status_label"] = {"available": tr("available"), "busy": tr("busy"), "offline": tr("offline")}.get(w.get("status", ""), "")
    w["loy"], w["loy_color"] = loyalty(w.get("jobs", 0))
    w["trust"] = trust_score(w)
    if w.get("rating", 0) >= 4.8 and w.get("jobs", 0) >= 20:
        w["special"] = "🏆 Top Rated"
    elif w.get("jobs", 0) >= 30:
        w["special"] = "💼 Most Reliable"
    elif w.get("status") == "available":
        w["special"] = "⚡ Fast Responder"
    else:
        w["special"] = ""
    return w

def enrich_j(j):
    j = dict(j)
    j["status_label"] = {
        "open": tr("status_open"),
        "inprog": tr("status_inprog"),
        "done": tr("status_done"),
    }.get(j.get("status", "open"), "")
    return j

def save_upload(file_obj, prefix=""):
    """Save uploaded image file. Returns filename or ''."""
    if not file_obj or not file_obj.filename:
        return ""
    ext = file_obj.filename.rsplit(".", 1)[-1].lower()
    if ext not in ("jpg", "jpeg", "png", "gif", "webp"):
        return ""
    fname = f"{prefix}{uuid.uuid4().hex[:12]}.{ext}"
    file_obj.save(os.path.join(UPLOAD_FOLDER, fname))
    return fname

def validate_cni(file_path):
    """
    Validates that the uploaded image is a landscape card (Cameroon CNI format).
    Cameroon CNI: 85.6mm × 54mm, landscape, aspect ratio ≈ 1.58.
    Returns (is_valid: bool, reason: str)
    """
    try:
        from PIL import Image
        img = Image.open(file_path)
        w, h = img.size
        if h >= w:
            return False, (
                "Image is portrait (vertical). Your CNI card must be placed horizontally "
                "(landscape). Please retake the photo with the card lying flat, wider than tall."
            )
        ratio = w / h
        if ratio < 1.1:
            return False, "Image is too square. Please photograph only the CNI card in landscape orientation."
        if ratio > 2.8:
            return False, "Image appears too wide. Please crop to show only the CNI card."
        if os.path.getsize(file_path) < 4000:
            return False, "Image file is too small or blank. Please upload a clear, in-focus photo."
        return True, "OK"
    except Exception as e:
        return False, f"Cannot read image: {e}. Please upload JPG or PNG format."

def send_verification_email(to_email, token, name):
    """Send real verification email via Resend. Returns (success, error_msg)."""
    if not RESEND_API_KEY:
        return False, "RESEND_API_KEY environment variable is not set."
    try:
        import requests as _r
        verify_url = f"{APP_URL}/verify-email/{token}"
        html = f"""
        <div style="font-family:Arial,sans-serif;max-width:520px;margin:auto;
                    padding:28px;border:1px solid #e0e0e0;border-radius:14px">
          <div style="text-align:center;margin-bottom:20px">
            <span style="font-size:48px">🏗️</span>
            <h2 style="color:#1a5c1a;margin:8px 0">Welcome to MboaLink, {name}!</h2>
          </div>
          <p style="color:#333;font-size:15px;line-height:1.7">
            Thank you for registering as a worker on MboaLink.<br/>
            Click the button below to verify your email and activate your profile.
          </p>
          <div style="text-align:center;margin:28px 0">
            <a href="{verify_url}"
               style="background:#1a5c1a;color:#fff;padding:15px 36px;border-radius:9px;
                      text-decoration:none;font-size:16px;font-weight:bold;display:inline-block">
              ✅ Verify My Email & Activate Account
            </a>
          </div>
          <p style="color:#888;font-size:12px">
            If the button doesn't work, copy and open this link:<br/>
            <a href="{verify_url}" style="color:#1a5c1a">{verify_url}</a>
          </p>
          <hr style="border:none;border-top:1px solid #eee;margin:20px 0"/>
          <p style="color:#aaa;font-size:11px;text-align:center">
            MboaLink — Connecting Cameroon's skilled workers with clients
          </p>
        </div>
        """
        resp = _r.post(
            "https://api.resend.com/emails",
            headers={
                "Authorization": f"Bearer {RESEND_API_KEY}",
                "Content-Type": "application/json",
            },
            json={
                "from": RESEND_FROM,
                "to": [to_email],
                "subject": "✅ Activate your MboaLink Worker Account",
                "html": html,
            },
            timeout=12,
        )
        if resp.status_code in (200, 201):
            return True, None
        return False, f"Resend API error {resp.status_code}: {resp.text[:200]}"
    except Exception as e:
        return False, str(e)

# ── CHAT HELPERS ──────────────────────────────────────────────────────────────
def get_or_create_chat(worker_id, client_email, client_name, worker_name, job_id=None):
    """Find existing chat between client & worker, or create one. Returns chat_id."""
    for cid, chat in CHATS.items():
        if chat["worker_id"] == worker_id and chat["client_email"] == client_email:
            return cid
    cid = "chat_" + uuid.uuid4().hex[:8]
    CHATS[cid] = {
        "worker_id": worker_id,
        "client_email": client_email,
        "client_name": client_name,
        "worker_name": worker_name,
        "job_id": job_id,
        "job_title": None,
        "messages": [],
        "created": datetime.datetime.now().strftime("%d/%m/%Y %H:%M"),
        "status": "open",  # open | agreed | done
    }
    # Auto-send a system greeting
    CHATS[cid]["messages"].append({
        "sender": "system",
        "text": f"Chat started between {client_name} and {worker_name}. "
                f"Discuss job details and agree on price and payment method here.",
        "time": datetime.datetime.now().strftime("%H:%M"),
        "type": "system",
    })
    return cid

def my_chats():
    """Return list of chats for the current user."""
    utype = session.get("utype", "client")
    result = []
    if utype == "worker":
        wid = session.get("worker_id", "")
        for cid, chat in CHATS.items():
            if chat["worker_id"] == wid:
                result.append({"id": cid, **chat})
    else:
        email = session.get("email", "")
        for cid, chat in CHATS.items():
            if chat["client_email"] == email:
                result.append({"id": cid, **chat})
    result.sort(key=lambda x: x.get("created", ""), reverse=True)
    return result

def ai_score(w, city):
    sc = int(w.get("rating", 0) * 8)
    reasons = []
    if w.get("rating", 0) >= 4.5: reasons.append("⭐ Highly rated")
    if w.get("status") == "available": sc += 25; reasons.append("✅ Available now")
    if w.get("city", "").lower() == city.lower(): sc += 20; reasons.append("📍 Same city")
    if w.get("cni_verified"): sc += 15; reasons.append("🔒 ID Verified")
    sc += min(w.get("jobs", 0) // 4, 15)
    return sc, reasons

# ── AUTH ROUTES ───────────────────────────────────────────────────────────────
@app.route("/lang/<l>")
def set_lang(l):
    if l in ("en", "fr", "pid"):
        session["lang"] = l
        session.modified = True  # force Flask to save session immediately
    return redirect(request.referrer or url_for("home"))

@app.route("/")
def index():
    return redirect(url_for("home") if session.get("logged_in") else url_for("login"))

@app.route("/login", methods=["GET", "POST"])
def login():
    err = ""
    if request.method == "POST":
        name  = request.form.get("name", "").strip()
        phone = request.form.get("phone", "").strip()
        city  = request.form.get("city", "Yaounde")
        utype = request.form.get("utype", "client")
        digits = "".join(c for c in phone if c.isdigit())
        if not name:
            err = "Please enter your full name."
        elif len(digits) < 9:
            err = "Please enter a valid phone number (at least 9 digits)."
        elif utype == "worker":
            session.update(reg_name=name, reg_phone=phone, reg_city=city)
            return redirect(url_for("register_worker"))
        else:
            email = f"client_{uuid.uuid4().hex[:6]}@mboa.cm"
            session.update(
                logged_in=True, name=name, phone=phone, city=city,
                utype="client", email=email, lang=session.get("lang", "en"),
            )
            return redirect(url_for("home"))
    return render_template("login.html", cities=CITIES, err=err, t=tr, lang=lang())

@app.route("/register-worker", methods=["GET", "POST"])
def register_worker():
    err = ""
    if request.method == "POST":
        email  = request.form.get("email", "").strip().lower()
        trade  = request.form.get("trade", "")
        bio    = request.form.get("bio", "").strip()
        pmin   = int(request.form.get("price_min", "5000") or 5000)
        pmax   = int(request.form.get("price_max", "50000") or 50000)

        if not email or "@" not in email or "." not in email.split("@")[-1]:
            err = "Please enter a valid email address."
        elif not trade:
            err = "Please select your trade."
        elif not bio or len(bio) < 20:
            err = "Please write a bio of at least 20 characters."
        else:
            # Profile picture (required)
            pic_file = request.files.get("profile_pic")
            if not pic_file or not pic_file.filename:
                err = "A profile photo is required."
            else:
                pic_fname = save_upload(pic_file, "profile_")
                if not pic_fname:
                    err = "Profile photo must be JPG, PNG, GIF or WEBP."
                else:
                    # CNI photo
                    cni_file = request.files.get("cni_photo")
                    if not cni_file or not cni_file.filename:
                        err = "Please upload a photo of your CNI (National ID card)."
                    else:
                        cni_ext = cni_file.filename.rsplit(".", 1)[-1].lower()
                        if cni_ext not in ("jpg", "jpeg", "png", "webp"):
                            err = "CNI photo must be JPG or PNG."
                        else:
                            cni_fname = f"cni_{uuid.uuid4().hex[:12]}.{cni_ext}"
                            cni_path  = os.path.join(UPLOAD_FOLDER, cni_fname)
                            cni_file.save(cni_path)
                            valid, reason = validate_cni(cni_path)
                            if not valid:
                                os.remove(cni_path)
                                try: os.remove(os.path.join(UPLOAD_FOLDER, pic_fname))
                                except: pass
                                err = f"❌ CNI rejected: {reason}"
                            else:
                                token = "".join(random.choices(
                                    string.ascii_letters + string.digits, k=52))
                                verify_tokens[token] = {
                                    "email": email,
                                    "name": session.get("reg_name", ""),
                                    "phone": session.get("reg_phone", ""),
                                    "city": session.get("reg_city", "Yaounde"),
                                    "trade": trade, "bio": bio,
                                    "pmin": pmin, "pmax": pmax,
                                    "pic": pic_fname, "cni_pic": cni_fname,
                                }
                                ok, send_err = send_verification_email(
                                    email, token, session.get("reg_name", "Worker"))
                                session["pending_email"] = email
                                session["pending_token"] = token
                                if not ok:
                                    session["email_send_error"] = send_err
                                return redirect(url_for("verify_worker_page"))

    return render_template("register_worker.html",
        trades=TRADES[lang()],
        name=session.get("reg_name", ""),
        phone=session.get("reg_phone", ""),
        city=session.get("reg_city", ""),
        err=err, t=tr, lang=lang())

@app.route("/verify-worker")
def verify_worker_page():
    email      = session.get("pending_email", "")
    token      = session.get("pending_token", "")
    send_error = session.pop("email_send_error", None)
    return render_template("verify_worker.html",
        email=email, token=token, send_error=send_error,
        resend_key_set=bool(RESEND_API_KEY), t=tr, lang=lang())

@app.route("/verify-email/<token>")
def verify_email(token):
    data = verify_tokens.get(token)
    if not data:
        flash("Invalid or expired verification link. Please register again.", "err")
        return redirect(url_for("login"))

    wid       = "w_" + uuid.uuid4().hex[:8]
    trade_raw = data["trade"]
    trade_en  = FR_TO_EN.get(trade_raw, trade_raw)  # normalise to English
    trade_fr  = EN_TO_FR.get(trade_en, trade_en)

    new_worker = {
        "id": wid,
        "name": data["name"],
        "trade_en": trade_en, "trade_fr": trade_fr,
        "emoji": TRADE_ICONS.get(trade_en, "🛠"),
        "city": data["city"], "phone": data["phone"], "email": data["email"],
        "cni_verified": True, "email_verified": True,
        "rating": 0.0, "jobs": 0, "ratings": 0, "status": "available",
        "price_min": data["pmin"], "price_max": data["pmax"],
        "bio_en": data["bio"], "bio_fr": data["bio"],
        "cert_en": "", "cert_fr": "", "badges": [],
        "pic": data["pic"], "cni_pic": data["cni_pic"],
    }
    WORKERS.append(new_worker)
    del verify_tokens[token]

    session.update(
        logged_in=True, name=data["name"], phone=data["phone"],
        city=data["city"], utype="worker", email=data["email"],
        worker_id=wid, lang=session.get("lang", "en"),
    )
    flash("✅ Email verified! Your worker profile is now live.", "ok")
    return redirect(url_for("worker_dashboard"))

@app.route("/logout")
def logout():
    session.clear()
    return redirect(url_for("login"))

# ── CLIENT ROUTES ─────────────────────────────────────────────────────────────
@app.route("/home")
def home():
    if session.get("utype") == "worker":
        return redirect(url_for("worker_dashboard"))
    top = [enrich_w(w) for w in sorted(WORKERS, key=lambda x: x.get("rating", 0), reverse=True)[:4]]
    stats = dict(
        workers=len(WORKERS),
        jobs=sum(1 for j in JOBS if j.get("status") == "open"),
        cities=len(CITIES),
    )
    notifs = notifications.get(session.get("email", ""), [])[:5]
    chats  = my_chats()
    return render_template("home.html", top=top, stats=stats, t=tr, lang=lang(),
                           name=session.get("name", ""), city=session.get("city", ""),
                           notifs=notifs, chats=chats)

@app.route("/workers")
def workers_list():
    q     = request.args.get("q", "").lower()
    trade = request.args.get("trade", "")
    city  = request.args.get("city", "")
    res   = [enrich_w(w) for w in WORKERS]
    if q:     res = [w for w in res if q in w["name"].lower() or q in w.get("trade","").lower() or q in w["city"].lower()]
    if trade: res = [w for w in res if w.get("trade_en","").lower() == trade.lower() or w.get("trade","").lower() == trade.lower()]
    if city:  res = [w for w in res if w["city"] == city]
    return render_template("workers.html", workers=res, trades=TRADES[lang()], cities=CITIES,
                           q=request.args.get("q",""), sel_trade=trade, sel_city=city,
                           t=tr, lang=lang())

@app.route("/worker/<wid>")
def worker_detail(wid):
    raw = next((w for w in WORKERS if w["id"] == wid), None)
    if not raw:
        return redirect(url_for("workers_list"))
    w    = enrich_w(raw)
    revs = [r for r in REVIEWS if r["wid"] == wid]
    port = portfolio_db.get(wid, [])
    return render_template("worker_detail.html", w=w, reviews=revs,
                           price=f"{w['price_min']:,}–{w['price_max']:,} XAF",
                           portfolio=port, t=tr, lang=lang())

@app.route("/jobs")
def jobs_list():
    return render_template("jobs.html",
                           jobs=[enrich_j(j) for j in JOBS],
                           t=tr, lang=lang())

@app.route("/job/<jid>")
def job_detail(jid):
    raw = next((j for j in JOBS if j["id"] == jid), None)
    if not raw:
        return redirect(url_for("jobs_list"))
    job_msgs = []
    for chat in CHATS.values():
        if chat.get("job_id") == jid:
            job_msgs = chat["messages"]
            break
    return render_template("job_detail.html", job=enrich_j(raw),
                           messages=job_msgs, t=tr, lang=lang(),
                           is_worker=session.get("utype") == "worker")

@app.route("/post-job", methods=["GET", "POST"])
def post_job():
    msg = ""
    if request.method == "POST":
        title = request.form.get("title", "").strip()
        desc  = request.form.get("desc", "").strip()
        trade = request.form.get("trade", "")
        city  = request.form.get("city", "")
        addr  = request.form.get("address", "").strip()
        bgt   = request.form.get("budget", "0").strip()
        emerg = "emergency" in request.form
        grp   = "group" in request.form
        wn    = int(request.form.get("wn", "1") or 1)
        if title and desc and bgt:
            JOBS.append({
                "id": uuid.uuid4().hex[:8],
                "title": title, "desc": desc, "trade": trade,
                "client": session.get("name", ""),
                "client_email": session.get("email", ""),
                "client_phone": session.get("phone", ""),
                "city": city, "address": addr,
                "budget": int(bgt), "emergency": emerg,
                "group": grp, "workers_needed": wn,
                "status": "open",
                "created": datetime.datetime.now().strftime("%d/%m/%Y"),
            })
            msg = tr("job_success")
    return render_template("post_job.html", trades=TRADES[lang()], cities=CITIES,
                           msg=msg, prefill=session.get("city", "Yaounde"),
                           pg=PRICE_GUIDE, t=tr, lang=lang())

@app.route("/emergency", methods=["GET", "POST"])
def emergency():
    result = None
    if request.method == "POST":
        te   = request.form.get("trade", "")
        city = request.form.get("city", session.get("city", "Yaounde"))
        cands = []
        for w in WORKERS:
            if w.get("trade_en","").lower()==te.lower() or w.get("trade_fr","").lower()==te.lower():
                sc, reasons = ai_score(w, city)
                cands.append((w, sc, reasons))
        if cands:
            best = max(cands, key=lambda x: x[1])
            result = {"worker": enrich_w(best[0]), "score": best[1], "reasons": best[2]}
    return render_template("emergency.html", trades=TRADES[lang()], cities=CITIES,
                           result=result, user_city=session.get("city","Yaounde"),
                           t=tr, lang=lang())

# ── CHAT / BOOKING ────────────────────────────────────────────────────────────
@app.route("/book/<wid>")
def start_booking(wid):
    """Client clicks 'Book & Chat' on a worker profile — opens/creates a chat."""
    if not session.get("logged_in"):
        return redirect(url_for("login"))
    raw = next((w for w in WORKERS if w["id"] == wid), None)
    if not raw:
        return redirect(url_for("workers_list"))
    job_id = request.args.get("job_id")
    cid = get_or_create_chat(
        worker_id=wid,
        client_email=session.get("email", ""),
        client_name=session.get("name", ""),
        worker_name=raw["name"],
        job_id=job_id,
    )
    add_notif(raw["email"],
        f"💬 {session.get('name','')} wants to book you! Check your chat.")
    return redirect(url_for("chat_view", cid=cid))

@app.route("/chat/<cid>", methods=["GET", "POST"])
def chat_view(cid):
    if not session.get("logged_in"):
        return redirect(url_for("login"))
    chat = CHATS.get(cid)
    if not chat:
        flash("Chat not found.", "err")
        return redirect(url_for("home"))

    # Permission check
    utype = session.get("utype", "client")
    if utype == "worker" and chat["worker_id"] != session.get("worker_id"):
        flash("Access denied.", "err")
        return redirect(url_for("home"))
    if utype == "client" and chat["client_email"] != session.get("email"):
        flash("Access denied.", "err")
        return redirect(url_for("home"))

    # Who's the other party?
    if utype == "worker":
        other_name  = chat["client_name"]
        other_email = chat["client_email"]
        worker_raw  = next((w for w in WORKERS if w["id"] == chat["worker_id"]), None)
        other_phone = ""  # client phone not stored (can add)
    else:
        other_name  = chat["worker_name"]
        other_email = ""
        worker_raw  = next((w for w in WORKERS if w["id"] == chat["worker_id"]), None)
        other_phone = worker_raw["phone"] if worker_raw else ""

    if request.method == "POST":
        action = request.form.get("action", "message")
        if action == "message":
            text = request.form.get("text", "").strip()
            if text:
                sender_name = session.get("name", "")
                chat["messages"].append({
                    "sender": sender_name,
                    "sender_type": utype,
                    "text": text,
                    "time": datetime.datetime.now().strftime("%H:%M"),
                    "type": "text",
                })
                # Notify the other person
                if utype == "worker":
                    add_notif(other_email, f"💬 {sender_name}: {text[:50]}")
                else:
                    if worker_raw:
                        add_notif(worker_raw["email"], f"💬 {sender_name}: {text[:50]}")
        elif action == "agree":
            # Mark booking as agreed
            chat["status"] = "agreed"
            agreed_price   = request.form.get("agreed_price", "")
            agreed_method  = request.form.get("agreed_method", "")
            note = f"✅ Booking agreed! Price: {agreed_price} XAF | Payment: {agreed_method}"
            chat["messages"].append({
                "sender": "system", "sender_type": "system",
                "text": note, "time": datetime.datetime.now().strftime("%H:%M"),
                "type": "agreed",
                "agreed_price": agreed_price, "agreed_method": agreed_method,
            })
            if worker_raw:
                add_notif(worker_raw["email"],
                    f"🤝 Booking confirmed with {chat['client_name']}! Price: {agreed_price} XAF")
            add_notif(chat["client_email"],
                f"🤝 {chat['worker_name']} confirmed your booking! Price: {agreed_price} XAF")
        return redirect(url_for("chat_view", cid=cid))

    worker_enriched = enrich_w(worker_raw) if worker_raw else {}
    return render_template("chat.html",
        chat=chat, cid=cid,
        worker=worker_enriched,
        other_name=other_name,
        other_phone=other_phone,
        utype=utype,
        my_name=session.get("name", ""),
        t=tr, lang=lang())

@app.route("/my-chats")
def my_chats_page():
    if not session.get("logged_in"):
        return redirect(url_for("login"))
    chats = my_chats()
    return render_template("my_chats.html", chats=chats, t=tr, lang=lang())

# ── WORKER ROUTES ─────────────────────────────────────────────────────────────
@app.route("/worker-dashboard")
def worker_dashboard():
    if session.get("utype") != "worker":
        return redirect(url_for("home"))
    wid    = session.get("worker_id", "")
    me     = next((w for w in WORKERS if w["id"] == wid), None)
    revs   = [r for r in REVIEWS if r["wid"] == wid]
    port   = portfolio_db.get(wid, [])
    jobs   = [enrich_j(j) for j in JOBS if j.get("status") == "open"]
    notifs = notifications.get(session.get("email", ""), [])[:8]
    chats  = my_chats()
    return render_template("worker_dashboard.html",
        worker=enrich_w(me) if me else None,
        reviews=revs, portfolio=port,
        open_jobs=jobs, notifs=notifs, chats=chats,
        t=tr, lang=lang())

@app.route("/worker-profile", methods=["GET", "POST"])
def worker_profile():
    if session.get("utype") != "worker":
        return redirect(url_for("home"))
    wid = session.get("worker_id", "")
    me  = next((w for w in WORKERS if w["id"] == wid), None)
    if not me:
        return redirect(url_for("home"))
    err = ""; success = ""
    if request.method == "POST":
        action = request.form.get("action", "update")
        if action == "update":
            bio = request.form.get("bio", "").strip()
            if bio: me["bio_en"] = bio; me["bio_fr"] = bio
            pmin = request.form.get("price_min", "")
            pmax = request.form.get("price_max", "")
            if pmin: me["price_min"] = int(pmin)
            if pmax: me["price_max"] = int(pmax)
            phone = request.form.get("phone", "").strip()
            if phone: me["phone"] = phone
            st = request.form.get("status", "")
            if st in ("available", "busy", "offline"):
                me["status"] = st
            pic_file = request.files.get("profile_pic")
            if pic_file and pic_file.filename:
                fname = save_upload(pic_file, "profile_")
                if fname:
                    me["pic"] = fname
                    success = "Profile updated with new photo!"
                else:
                    err = "Photo must be JPG, PNG, GIF or WEBP."
            else:
                success = "Profile updated!"

        elif action == "add_portfolio":
            title = request.form.get("port_title", "").strip()
            desc  = request.form.get("port_desc", "").strip()
            if not title:
                err = "Portfolio item needs a title."
            else:
                port_file = request.files.get("port_photo")
                port_pic  = save_upload(port_file, "portfolio_") if port_file else ""
                portfolio_db.setdefault(wid, []).append({
                    "id":    uuid.uuid4().hex[:6],
                    "title": title, "desc": desc, "pic": port_pic,
                    "date":  datetime.datetime.now().strftime("%d/%m/%Y"),
                })
                success = "Portfolio item added!"

        elif action == "delete_portfolio":
            pid = request.form.get("port_id", "")
            portfolio_db[wid] = [p for p in portfolio_db.get(wid, []) if p["id"] != pid]
            success = "Item removed."

    return render_template("worker_profile.html",
        worker=enrich_w(me),
        portfolio=portfolio_db.get(wid, []),
        err=err, success=success, t=tr, lang=lang())

@app.route("/apply-job/<jid>", methods=["POST"])
def apply_job(jid):
    if session.get("utype") != "worker":
        return redirect(url_for("home"))
    raw = next((j for j in JOBS if j["id"] == jid), None)
    if raw:
        add_notif(raw.get("client_email", ""),
            f"👷 {session.get('name','')} applied to your job: {raw.get('title','')}")
        flash("Application sent! The client can now start a chat with you.", "ok")
    return redirect(url_for("job_detail", jid=jid))

@app.route("/profile")
def profile():
    if session.get("utype") == "worker":
        return redirect(url_for("worker_profile"))
    notifs = notifications.get(session.get("email", ""), [])
    return render_template("profile.html", t=tr, lang=lang(),
                           name=session.get("name", ""),
                           phone=session.get("phone", ""),
                           city=session.get("city", ""),
                           utype=session.get("utype", "client"),
                           notifs=notifs)

# ── REVIEWS ───────────────────────────────────────────────────────────────────
@app.route("/review/<wid>", methods=["GET", "POST"])
def leave_review(wid):
    worker = next((w for w in WORKERS if w["id"] == wid), None)
    if not worker:
        return redirect(url_for("workers_list"))
    err = ""; success = ""
    if request.method == "POST":
        rating  = int(request.form.get("rating", "5"))
        comment = request.form.get("comment", "").strip()
        badges_awarded = request.form.getlist("badges")
        if not comment:
            err = "Please write a comment."
        else:
            REVIEWS.append({
                "wid": wid, "from_name": session.get("name", "Anonymous"),
                "rating": rating, "comment": comment,
                "date": datetime.datetime.now().strftime("%d/%m/%Y"),
            })
            wrevs = [r for r in REVIEWS if r["wid"] == wid]
            worker["rating"]  = round(sum(r["rating"] for r in wrevs) / len(wrevs), 1)
            worker["ratings"] = len(wrevs)
            worker["jobs"]    = worker.get("jobs", 0) + 1
            for b in badges_awarded:
                if b not in worker["badges"]:
                    worker["badges"].append(b)
            if worker["jobs"] >= 50 and "Gold Worker 🏆" not in worker["badges"]:
                worker["badges"].append("Gold Worker 🏆")
            elif worker["jobs"] >= 20 and "Silver Worker 🥈" not in worker["badges"]:
                worker["badges"].append("Silver Worker 🥈")
            add_notif(worker.get("email", ""),
                f"⭐ New {rating}-star review from {session.get('name', '')}!")
            success = "Review submitted! Thank you."
    return render_template("leave_review.html", worker=enrich_w(worker),
                           badge_options=BADGE_OPTIONS, err=err, success=success,
                           t=tr, lang=lang())

# ── AI & PRICE APIS ───────────────────────────────────────────────────────────
@app.route("/api/ai-match")
def api_ai():
    te   = request.args.get("trade", "")
    city = request.args.get("city", session.get("city", "Yaounde"))
    out  = []
    for w in WORKERS:
        if w.get("trade_en","").lower() != te.lower():
            continue
        sc, reasons = ai_score(w, city)
        ew = enrich_w(w)
        out.append({"id": w["id"], "name": w["name"], "trade": ew["trade"],
                    "rating": w.get("rating", 0), "city": w["city"], "phone": w["phone"],
                    "status": ew["status_label"], "score": sc, "reasons": reasons})
    out.sort(key=lambda x: x["score"], reverse=True)
    return jsonify(out[:5])

@app.route("/api/price/<trade>")
def api_price(trade):
    return jsonify({"price": PRICE_GUIDE.get(trade, "Variable")})

if __name__ == "__main__":
    print("\n" + "=" * 58)
    print("  MboaLink v5  →  http://localhost:5000")
    if not RESEND_API_KEY:
        print()
        print("  ⚠️  EMAIL NOT CONFIGURED")
        print("  1. Sign up FREE at https://resend.com")
        print("  2. Copy your API key (starts with re_)")
        print("  3. Run:")
        print("     RESEND_API_KEY=re_xxxx python app.py")
        print()
        print("  Until then, use the manual verify button on screen.")
    else:
        print(f"  ✅ Email: Resend configured ({RESEND_FROM})")
    print("=" * 58 + "\n")
    app.run(debug=True, port=5000)
