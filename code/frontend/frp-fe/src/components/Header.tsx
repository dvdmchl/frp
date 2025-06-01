import { useTranslation } from "react-i18next";
import type { UserDto } from "../api/models/UserDto";

type HeaderProps = {
    user?: UserDto | null;
};

// Pro zjednodušení předpokládáme, že user je předáván přes props nebo context:
export default function Header({ user }: HeaderProps) {
    const {i18n } = useTranslation();

    const handleLanguageChange = (lng: string) => {
        i18n.changeLanguage(lng);
        localStorage.setItem("lang", lng);
    };

    // const handleLogout = () => {
    //     setUser(null);
    //     localStorage.removeItem("jwt");
    //     window.location.href = "/login";
    // };

    return (
        <header className="bg-blue-600 text-white px-4 py-3 flex items-center justify-between shadow-md">
            <div className="text-lg font-bold">FRP App</div>
            <div className="flex items-center gap-4">
                <select
                    value={i18n.language}
                    onChange={e => handleLanguageChange(e.target.value)}
                    className="text-black px-2 py-1 rounded"
                >
                    <option value="cs">Čeština</option>
                    <option value="en">English</option>
                </select>
                {user && (
                    <div className="font-semibold">{user.fullName}</div>
                )}
            </div>
        </header>
    );
}
