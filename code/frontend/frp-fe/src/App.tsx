import './App.css'
import {BrowserRouter, Navigate, Route, Routes} from "react-router-dom";
import {LoginForm} from "./components/UserManagement/LoginForm";
import {RegisterForm} from "./components/UserManagement/RegisterForm";
import {useState, useEffect} from "react";
import type {UserDto} from "./api/models/UserDto.ts";
import type {UserLoginResponseDto} from "./api/models/UserLoginResponseDto";
import {HomePage} from "./components/HomePage.tsx";
import Header from "./components/Header";
import Footer from "./components/Footer";
import {OpenAPI, UserManagementService} from "./api";

function App() {
    const [user, setUser] = useState<UserDto | null>(null);

    useEffect(() => {
        const jwt = localStorage.getItem("jwt");
        if (jwt && !user) {
            OpenAPI.TOKEN = jwt;
            // zavolej /me
            UserManagementService.authenticatedUser()
                .then(u => setUser(u))
                .catch(() => {
                    setUser(null);
                    localStorage.removeItem("jwt");
                });
        }
    }, []);

    const handleLoginSuccess = (loginResp: UserLoginResponseDto) => {
        if (!loginResp.user) {
            console.error("Login response does not contain user data");
            return;
        }
        console.log("Login successful:", loginResp);
        setUser(loginResp.user);
        if (loginResp.token != null) {
            localStorage.setItem("jwt", loginResp.token);
            OpenAPI.TOKEN = loginResp.token;
        }
    };

    const handleRegisterSuccess = () => {
        // Po registraci můžeš:
        // - přesměrovat na login
        // - nebo rovnou přihlásit (pokud ti /register vrací uživatele+token)
        window.location.href = "/login"; // nebo useNavigate("/login")
    };

    return (
        <div className="min-h-screen flex flex-col">
            <Header user={user || undefined} />
            <main className="flex-grow">
                <BrowserRouter>
                    <Routes>
                        {!user && (
                            <>
                                <Route path="/login" element={<LoginForm onLoginSuccess={handleLoginSuccess} />} />
                                <Route path="/register" element={<RegisterForm onRegisterSuccess={handleRegisterSuccess} />} />
                                <Route path="*" element={<Navigate to="/login" />} />
                            </>
                        )}
                        {user && (
                            <>
                                <Route path="/*" element={<HomePage user={user} />} />
                            </>
                        )}
                    </Routes>
                </BrowserRouter>
            </main>
            <Footer />
        </div>
    );
}

export default App
