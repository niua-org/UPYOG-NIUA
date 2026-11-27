// generated-by: generate_node_unit_tests.py
describe('../../../src/utils/notificationUtil', () => {
  test('module_whenRequired_loadsWithoutThrow', () => {
    jest.resetModules();
    let mod;
    expect(() => {
      mod = require('../../../src/utils/notificationUtil');
    }).not.toThrow();
    expect(mod).toBeDefined();
    
  });
});
