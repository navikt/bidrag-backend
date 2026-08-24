export function groupBy<T, K extends keyof never>(
  list: T[],
  getKey: (item: T) => K,
): [string, T[]][] {
  return Object.entries(
    list.reduce(
      (previous, currentItem) => {
        const group = getKey(currentItem);
        if (!previous[group]) previous[group] = [];
        previous[group].push(currentItem);
        return previous;
      },
      {} as Record<K, T[]>,
    ),
  );
}
export const hasValue = <T>(array: T[], key: string): boolean => {
  return array.some((item) => item === key);
};
