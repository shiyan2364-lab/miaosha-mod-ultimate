# 秒杀模组 Ultimate (miaosha-mod-ultimate)

Minecraft 1.16.5 Fabric 秒杀模组（终极版）

## 功能

- 独立物品栏类别「秒杀武器」
- 秒杀之剑 (miaosha_sword)：一击必杀，掉落物/经验正常
- 湮灭之剑 (miaosha_erase_sword)：深层硬抹除
  - 反射改血（绕锁血）
  - 移入虚空 + remove + 反射 removed
  - 持续拦截 tick/setHealth/getHealth/revive/掉落（防拉血、防复活、无掉落）
- 攻速：秒杀之剑 1.6 / 湮灭之剑 3.6（极快）

## 依赖

- Minecraft 1.16.5
- Fabric Loader >= 0.15.11
- Fabric API 0.42.0+1.16
- Java 8+（FCL 手机端可用 JRE8）

## 构建

```bash
./gradlew build
```

产物在 `build/libs/`。

## 使用

放入 `.minecraft/mods/`，游戏内获取：

```
/give @s miaosha-mod-ultimate:miaosha_sword
/give @s miaosha-mod-ultimate:miaosha_erase_sword
```

## 贴图来源

- 秒杀之剑：prismal_abyss.png（ArcaneVortex 致敬）
- 湮灭之剑：entity_remover.png（ArcaneVortex 致敬）

## License

CC0-1.0
