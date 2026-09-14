import { Search } from 'lucide-react';
import { FormEvent, useState } from 'react';

type SearchBarProps = {
  onSubmit: (value: string) => void;
  placeholder?: string;
};

export function SearchBar({ onSubmit, placeholder = 'Rechercher un fichier' }: SearchBarProps) {
  const [value, setValue] = useState('');

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit(value.trim());
  }

  return (
    <form className="search-bar" onSubmit={handleSubmit} aria-label="Recherche de fichier">
      <Search aria-hidden="true" size={20} />
      <input
        type="search"
        aria-label="Rechercher"
        placeholder={placeholder}
        value={value}
        onChange={(event) => setValue(event.target.value)}
      />
      <button type="submit">Entrer</button>
    </form>
  );
}