export async function copyFieldValue(
  value: string,
  clipboard: { writeText(text: string): Promise<void> } = navigator.clipboard
): Promise<void> {
  await clipboard.writeText(value);
}
