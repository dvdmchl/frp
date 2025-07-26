import React from 'react';

type Item = {
    key: string;
    label: string;
};

type SideMenuProps = {
    items: Item[];
    selected: string;
    onSelect: (key: string) => void;
};

export function SideMenu({ items, selected, onSelect }: Readonly<SideMenuProps>) {
    return (
        <div className="w-48 flex flex-col bg-bgForm rounded-lg shadow-md">
            {items.map(item => (
                <button
                    key={item.key}
                    onClick={() => onSelect(item.key)}
                    className={`px-4 py-2 text-left border-b last:border-b-0 border-gray-200 hover:bg-primary hover:text-textLight ${selected === item.key ? 'bg-primary text-textLight' : ''}`}
                >
                    {item.label}
                </button>
            ))}
        </div>
    );
}
