import './App.css'
import {BrowserRouter} from "react-router-dom";
import {useState, useEffect} from "react";
import type {UserDto} from "./api/models/UserDto.ts";
import type {UserLoginResponseDto} from "./api/models/UserLoginResponseDto";
import Header from "./components/Header";
import Footer from "./components/Footer";
import {OpenAPI, UserManagementService} from "./api";
import {AppRoutes} from "./AppRoutes.tsx";
import { Spinner } from "flowbite-react";

function App() {
    const [user, setUser] = useState<UserDto | null>(null);
    const [loadingUser, setLoadingUser] = useState(true);

    useEffect(() => {
        const jwt = localStorage.getItem("jwt");
        if (jwt) {
            OpenAPI.TOKEN = jwt;
            UserManagementService.authenticatedUser()
                .then(u => setUser(u))
                .catch(() => {
                    setUser(null);
                    localStorage.removeItem("jwt");
                })
                .finally(() => {
                    setLoadingUser(false);
                });
        } else {
            setLoadingUser(false);
        }
    }, []);

    if (loadingUser) {
        return <Spinner/>;
    }

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
            <BrowserRouter>
                <Header user={user} onLogout={() => setUser(null)}/>
                <main className="flex-grow">

                    <AppRoutes
                        user={user}
                        onLoginSuccess={handleLoginSuccess}
                        onRegisterSuccess={handleRegisterSuccess}
                        setUser={setUser}
                    />

                </main>
                <Footer/>
            </BrowserRouter>
        </div>
    );
}

export default App
