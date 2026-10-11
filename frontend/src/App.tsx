import { Navigate, Route, Routes } from 'react-router'
import Layout from './components/Layout.tsx'
import ChatPage from './pages/ChatPage.tsx'
import ProgressPage from './pages/ProgressPage.tsx'
import SettingsPage from './pages/SettingsPage.tsx'

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to="/chat" replace />} />
        <Route path="chat" element={<ChatPage />} />
        <Route path="progress" element={<ProgressPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  )
}
