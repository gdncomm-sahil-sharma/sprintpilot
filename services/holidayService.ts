const API_BASE_URL = '/api/holidays';

export interface HolidayDto {
    id?: string;
    name: string;
    holidayDate: string;
    holidayType: 'PUBLIC' | 'COMPANY';
    recurring: boolean;
    location?: string[] | null; // Array of locations: ["BANGALORE", "COIMBATORE"] or null for global
}

interface ApiResponse<T> {
    success: boolean;
    message?: string;
    data?: T;
    error?: string;
}

async function fetchApi<T>(endpoint: string, options?: RequestInit): Promise<ApiResponse<T>> {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options?.headers,
            },
            ...options,
        });
        return await response.json();
    } catch (error) {
        console.error('API request failed:', error);
        return {
            success: false,
            error: error instanceof Error ? error.message : 'Unknown error',
        };
    }
}

export const getAllHolidays = async (location?: string): Promise<HolidayDto[]> => {
    const endpoint = location ? `?location=${location}` : '';
    const response = await fetchApi<HolidayDto[]>(endpoint);
    return response.success && response.data ? response.data : [];
};

export const getHolidayById = async (id: string): Promise<HolidayDto | null> => {
    const response = await fetchApi<HolidayDto>(`/${id}`);
    return response.success && response.data ? response.data : null;
};

export const createHoliday = async (holiday: Omit<HolidayDto, 'id'>): Promise<HolidayDto | null> => {
    const response = await fetchApi<HolidayDto>('', {
        method: 'POST',
        body: JSON.stringify(holiday),
    });
    return response.success && response.data ? response.data : null;
};

export const updateHoliday = async (id: string, holiday: Omit<HolidayDto, 'id'>): Promise<HolidayDto | null> => {
    const response = await fetchApi<HolidayDto>(`/${id}`, {
        method: 'PUT',
        body: JSON.stringify(holiday),
    });
    return response.success && response.data ? response.data : null;
};

export const deleteHoliday = async (id: string): Promise<boolean> => {
    const response = await fetchApi<void>(`/${id}`, {
        method: 'DELETE',
    });
    return response.success;
};

export const getHolidaysByDateRange = async (startDate: string, endDate: string, location?: string): Promise<HolidayDto[]> => {
    const locationParam = location ? `&location=${location}` : '';
    const response = await fetchApi<HolidayDto[]>(`/range?startDate=${startDate}&endDate=${endDate}${locationParam}`);
    return response.success && response.data ? response.data : [];
};

export const getHolidaysByYear = async (year: number, location?: string): Promise<HolidayDto[]> => {
    const locationParam = location ? `?location=${location}` : '';
    const response = await fetchApi<HolidayDto[]>(`/year/${year}${locationParam}`);
    return response.success && response.data ? response.data : [];
};

export const getRecurringHolidays = async (location?: string): Promise<HolidayDto[]> => {
    const locationParam = location ? `?location=${location}` : '';
    const response = await fetchApi<HolidayDto[]>(`/recurring${locationParam}`);
    return response.success && response.data ? response.data : [];
};

export const getHolidayDatesForSprint = async (startDate: string, endDate: string, location?: string): Promise<string[]> => {
    const locationParam = location ? `&location=${location}` : '';
    const response = await fetchApi<string[]>(`/sprint?startDate=${startDate}&endDate=${endDate}${locationParam}`);
    return response.success && response.data ? response.data : [];
};

export const isHoliday = async (date: string, location?: string): Promise<boolean> => {
    const locationParam = location ? `&location=${location}` : '';
    const response = await fetchApi<boolean>(`/check?date=${date}${locationParam}`);
    return response.success && response.data ? response.data : false;
};

export const getHolidaysByLocation = async (location: string): Promise<HolidayDto[]> => {
    const response = await fetchApi<HolidayDto[]>(`/location/${location}`);
    return response.success && response.data ? response.data : [];
};

