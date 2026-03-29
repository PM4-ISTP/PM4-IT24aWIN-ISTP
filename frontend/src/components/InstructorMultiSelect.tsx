'use client';

import {useCallback, useState} from 'react';
import {MultiSelect} from '@mantine/core';
import {useDebouncedCallback} from '@mantine/hooks';

interface Instructor {
    id: string;
    name: string;
}

interface InstructorMultiSelectProps {
    value: string[];
    onChange: (value: string[]) => void;
}

export function InstructorMultiSelect({value, onChange}: InstructorMultiSelectProps) {
    const [searchValue, setSearchValue] = useState('');
    const [options, setOptions] = useState<{ value: string; label: string }[]>([]);
    const [loading, setLoading] = useState(false);

    const fetchInstructors = useCallback(async (name: string) => {
        setLoading(true);
        try {
            const params = new URLSearchParams({
                ...(name ? {name} : {}),
                size: '20',
                page: '0',
            });

            const res = await fetch(`/api/users/instructors?${params}`);
            const data = await res.json();

            setOptions((prev) => {
                const selected = prev.filter((o) => value.includes(o.value));
                const newOpts = (data.content ?? []).map((u: Instructor) => ({value: u.id, label: u.name}));
                return [...selected, ...newOpts.filter((o: { value: string }) => !value.includes(o.value))];
            });
        } finally {
            setLoading(false);
        }
    }, [value]);

    const debouncedFetch = useDebouncedCallback(fetchInstructors, 300);

    const handleSearchChange = (val: string) => {
        setSearchValue(val);
        debouncedFetch(val);
    };

    const handleDropdownOpen = () => {
        if (options.length === 0) {
            fetchInstructors('');
        }
    };

    return (
        <MultiSelect
            label="Instructors"
            placeholder="Search instructors..."
            data={options}
            value={value}
            onChange={onChange}
            searchable
            searchValue={searchValue}
            onSearchChange={handleSearchChange}
            onDropdownOpen={handleDropdownOpen}
            nothingFoundMessage={loading ? 'Loading...' : 'No instructors found'}
            clearable
            hidePickedOptions
        />
    );
}