import { createContext, useContext, PropsWithChildren } from "react";

export type Styling = "V1" | "V2";
export type ThemeContextProps = {
  styling: Styling;
};
export const ThemeContext = createContext<ThemeContextProps | null>(null);
export function useTheme(): ThemeContextProps {
  return useContext(ThemeContext) as ThemeContextProps;
}

export function ThemeProvider({
  children,
  styling,
}: PropsWithChildren<ThemeContextProps>) {
  return (
    <ThemeContext.Provider
      value={{
        styling,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}
