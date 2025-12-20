type Item<T extends string> = {
  key: T
  label: string
}

type SideMenuProps<T extends string> = {
  items: Item<T>[]
  selected: T
  onSelect: (key: T) => void
}

export function SideMenu<T extends string>({ items, selected, onSelect }: SideMenuProps<T>) {
  return (
    <div className="w-48 flex flex-col bg-bgForm rounded-lg shadow-md">
      {items.map((item) => (
        <button
          key={item.key}
          onClick={() => onSelect(item.key)}
          className={`px-4 py-2 text-left border-b last:border-b-0 border-gray-200 hover:bg-primary hover:text-textLight ${selected === item.key ? 'bg-primary text-textLight' : ''}`}
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
