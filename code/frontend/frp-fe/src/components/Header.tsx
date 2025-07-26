import {useTranslation} from "react-i18next";
import type {UserDto} from "../api/models/UserDto";
import {HeaderTitle, LinkText} from "./UIComponent/Text.tsx";
import {LogoutButton} from "./UIComponent/Button.tsx";
import {useState} from "react";
import {UserManagementService} from "../api/services/UserManagementService";
import {useNavigate} from "react-router-dom";

type HeaderProps = {
    user?: UserDto | null;
    onLogout?: () => void;
};

// Pro zjednodušení předpokládáme, že user je předáván přes props nebo context:
export default function Header({user, onLogout}: Readonly<HeaderProps>) {
    const {i18n} = useTranslation();
    const navigate = useNavigate();
    const [loading, setLoading] = useState(false);

    const handleLanguageChange = (lng: string) => {
        i18n.changeLanguage(lng);
        localStorage.setItem("lang", lng);
    };

    const onLogoutClick = async () => {
        setLoading(true);
        try {
            await UserManagementService.logout();
            localStorage.removeItem("jwt");
            if (onLogout) {
                onLogout();
            }
            navigate("/login");
        } catch (err) {
            console.error("Logout failed", err);
        } finally {
            setLoading(false);
        }
    };

    return (
        <header className="flex flex-row bg-bgHeader">
            <div className="basis-2/3">
                <LinkText to="/"><HeaderTitle>Family Resource Planner</HeaderTitle></LinkText>
            </div>
            <div className="basis-1/3">
                <div className="flex space-x-4 items-center justify-end">
                    <div>
                        <select
                            value={i18n.language}
                            onChange={e => handleLanguageChange(e.target.value)}
                            className="text-black px-3 py-2 rounded-md border border-gray-300 focus:outline-none focus:ring-2 focus:ring-blue-400 transition"
                        >
                            <option value="cs">Čeština</option>
                            <option value="en">English</option>
                        </select>
                    </div>
                    <div>
                        {user && <LinkText to="/profile">{user.fullName}</LinkText>}
                    </div>
                    <div>
                        <span>selected schema</span>
                    </div>
                    <div>
                        <LogoutButton loading={loading} onClick={onLogoutClick}/>
                    </div>
                </div>
            </div>
        </header>
    );
}
