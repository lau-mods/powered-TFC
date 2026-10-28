const { scenario, command, camera, screenshot, mct } = await import(process.env.HARNESS_E2E_LIB);

await scenario(async ({ clients }) => {
  const client = clients[0];
  command(client, 'gamerule sendCommandFeedback false');
  command(client, 'gamemode spectator');
  command(client, 'fill -3 3 -1 3 3 4 minecraft:stone');
  command(client, 'setblock 0 5 1 minecraft:stone');
  command(client, 'setblock 0 5 2 minecraft:oak_wall_sign[facing=south]');
  mct(client, 'input', 'key', 'press', 'f1');

  command(client, 'setblock 0 5 0 tfc:crucible');
  command(client, 'data merge block 0 5 0 {temperature:1000f,targetTemperature:1000f}');
  command(client, 'setblock 0 4 0 create:display_link[facing=down,powered=false]');
  command(client, 'data merge block 0 4 0 {Source:{Id:"poweredtfc:tfc_crucible",Mode:0},TargetOffset:[I;0,1,2],TargetLine:0}');
  mct(client, 'wait', '2');
  await camera(client, { x: 0.5, y: 5.6, z: 5.5, yaw: 180, pitch: 4 });
  screenshot(client, 'AC-F008-001', 'crucible-celsius');

  command(client, 'data merge block 0 4 0 {Source:{Id:"poweredtfc:tfc_crucible",Mode:1}}');
  mct(client, 'wait', '2');
  await camera(client, { x: 0.5, y: 5.6, z: 5.5, yaw: 180, pitch: 4 });
  screenshot(client, 'AC-F008-001', 'crucible-temperature-color');

  command(client, 'data merge block 0 5 0 {temperature:0f,targetTemperature:0f}');
  mct(client, 'wait', '2');
  await camera(client, { x: 0.5, y: 5.6, z: 5.5, yaw: 180, pitch: 4 });
  screenshot(client, 'AC-F008-002', 'crucible-no-heat-after-update');

  command(client, 'fill -1 6 -1 1 6 1 tfc:reinforced_fire_bricks');
  command(client, 'setblock 0 6 0 air');
  command(client, 'setblock 0 5 0 tfc:blast_furnace[lit=true]');
  command(client, 'data merge block 0 5 0 {temperature:1000f,burnTemperature:1000f,burnTicks:10000,inputStacks:[{id:"tfc:metal/ingot/wrought_iron",count:1},{id:"tfc:metal/ingot/wrought_iron",count:1}],catalystStacks:[{id:"tfc:powder/flux",count:1},{id:"tfc:powder/flux",count:1}],fuelStacks:[{id:"minecraft:charcoal",count:1},{id:"minecraft:charcoal",count:1},{id:"minecraft:charcoal",count:1},{id:"minecraft:charcoal",count:1}]}');

  const modes = [
    ['temperature', 0],
    ['temperature-color', 1],
    ['input-count', 2],
    ['catalyst-count', 3],
    ['fuel-count', 4],
    ['compact', 5]
  ];
  for (const [label, mode] of modes) {
    command(client, `data merge block 0 4 0 {Source:{Id:"poweredtfc:tfc_blast_furnace",Mode:${mode}}}`);
    mct(client, 'wait', '2');
    await camera(client, { x: 0.5, y: 5.6, z: 5.5, yaw: 180, pitch: 4 });
    screenshot(client, 'AC-F009-001', `blast-furnace-${label}`);
  }

  command(client, 'data merge block 0 5 0 {temperature:1200f,burnTemperature:1200f,inputStacks:[{id:"tfc:metal/ingot/wrought_iron",count:1},{id:"tfc:metal/ingot/wrought_iron",count:1},{id:"tfc:metal/ingot/wrought_iron",count:1}],catalystStacks:[{id:"tfc:powder/flux",count:1},{id:"tfc:powder/flux",count:1},{id:"tfc:powder/flux",count:1}],fuelStacks:[{id:"minecraft:charcoal",count:1}]}');
  mct(client, 'wait', '2');
  await camera(client, { x: 0.5, y: 5.6, z: 5.5, yaw: 180, pitch: 4 });
  screenshot(client, 'AC-F009-002', 'blast-furnace-live-compact-update');
});
