// src/components/HomePage.tsx
import React from "react";
import type {UserDto} from "../api/models/UserDto";
import {H2Title, Paragraph} from "./UIComponent/Text.tsx";

export const HomePage: React.FC<{ user: UserDto }> = ({user}) => (
    <div className="grid">
        <div className="row">
            <H2Title>Vítej, {user.fullName}!</H2Title>
        </div>
        <div className="row">
            <Paragraph>Tohle je základní rozhraní pro přihlášeného uživatele.</Paragraph>
        </div>
    </div>
);
