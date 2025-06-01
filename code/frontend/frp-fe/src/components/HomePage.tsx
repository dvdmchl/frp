// src/components/HomePage.tsx
import React from "react";
import type {UserDto} from "../api/models/UserDto";

export const HomePage: React.FC<{ user: UserDto }> = ({ user }) => (
    <div className="max-w-2xl mx-auto mt-8 p-6 bg-white shadow-xl rounded-lg">
        <h1 className="text-2xl font-bold mb-4">Vítej, {user.fullName}!</h1>
        <p>Tohle je základní rozhraní pro přihlášeného uživatele.</p>
        {/* Tady pak přidáš odkazy, dashboard, atd. */}
    </div>
);
