const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamerule fallDamage false');
  command(client, 'gamemode spectator');
  command(client, 'fill 0 3 -6 8 3 2 minecraft:stone');
  command(client, 'setblock 2 4 -1 create:creative_motor[facing=south]');
  command(client, 'setblock 2 4 0 poweredtfc:create_to_tfc_converter[facing=north]');
  command(client, 'setblock 2 4 1 tfc:wood/axle/oak[axis=z]');
  command(client, 'setblock 6 4 -1 tfc:creative_motor[axis=z]');
  command(client, 'data merge block 6 4 -1 {step:8}');
  command(client, 'setblock 6 4 0 poweredtfc:tfc_to_create_converter[facing=south]');
  command(client, 'setblock 6 4 1 create:shaft[axis=z]');
  mct(client, 'input', 'key', 'press', 'f1');
  await camera(client, { x: 4.5, y: 5.8, z: -5.5, yaw: 0, pitch: 8 });

  command(client, 'setblock 2 5 0 minecraft:redstone_block');
  command(client, 'setblock 6 5 0 minecraft:redstone_block');
  await camera(client, { x: 4.5, y: 5.8, z: -5.5, yaw: 0, pitch: 8 });
  screenshot(client, 'AC-F005-001', 'powered-converters');

  command(client, 'setblock 2 5 0 minecraft:air');
  command(client, 'setblock 6 5 0 minecraft:air');
  await camera(client, { x: 4.5, y: 5.8, z: -5.5, yaw: 0, pitch: 8 });
  screenshot(client, 'AC-F005-002', 'resumed-converters');
});
