const { scenario, command, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamemode creative');
  command(client, 'clear @s');
  command(client, 'item replace entity @s hotbar.0 with create:wheat_flour');
  command(client, 'item replace entity @s hotbar.1 with create:basin');
  mct(client, 'input', 'key', 'press', 'e');
  mct(client, 'wait', '1');

  const layout = mct(client, 'gui', 'layout');
  const flour = layout.slots.find(slot => slot.item?.type === 'create:wheat_flour');
  const basin = layout.slots.find(slot => slot.item?.type === 'create:basin');
  if (!flour || !basin) {
    throw new Error('Create representative items were not present in the open inventory');
  }

  mct(client, 'input', 'mouse-move', String(flour.centerX), String(flour.centerY));
  screenshot(client, 'AC-F011-002', 'tiny-very-light-tooltip');

  mct(client, 'input', 'mouse-move', String(basin.centerX), String(basin.centerY));
  screenshot(client, 'AC-F011-002', 'huge-heavy-tooltip');
});
