const { app, BrowserWindow, Notification } = require('electron')

function createWindow() {
  const win = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 900,
    minHeight: 600,
    title: 'StudyPulse',
    backgroundColor: '#060b16',
    show: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
    }
  })

  win.loadFile('index.html')
  win.setMenuBarVisibility(false)
  win.once('ready-to-show', () => { win.show() })
}

app.whenReady().then(() => {
  createWindow()

  // System notification: due task reminder (every 30 mins)
  setInterval(() => {
    new Notification({
      title: 'StudyPulse Reminder 📚',
      body: 'You have incomplete tasks today. Keep going homie! 💪'
    }).show()
  }, 30 * 60 * 1000)

  // Show welcome notification on launch
  setTimeout(() => {
    new Notification({
      title: 'Welcome back homie! 👋',
      body: 'You have 3 goals remaining today. Lets get it!'
    }).show()
  }, 3000)
})

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit()
})
