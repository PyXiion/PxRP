import { defineConfig } from "astro/config";
import starlight from "@astrojs/starlight";

export default defineConfig({
  site: "https://ignis.pyxiion.ru",
  integrations: [
    starlight({
      title: "PxIgnis",
      description:
        "Async Lua scripting engine for Minecraft Fabric — coroutines, mob AI, hot reload.",
      social: [
        { icon: "github", label: "GitHub", href: "https://github.com/pyxiion/PxRP" },
        { icon: "discord", label: "Discord", href: "https://discord.gg/FyPWDheyzs" },
        { icon: "download", label: "Modrinth", href: "https://modrinth.com/mod/pxrp" },
      ],
      sidebar: [
        {
          label: "Getting Started",
          slug: "getting-started",
        },
        {
          label: "Guide",
          items: [
            { label: "Registering Commands", slug: "registering-commands" },
            { label: "Command Arguments", slug: "command-arguments" },
            { label: "Permissions", slug: "permissions" },
          ],
        },
        {
          label: "API Reference",
          items: [
            { label: "mc.* API", slug: "mc-api" },
            { label: "Player API", slug: "player-api" },
            { label: "World API", slug: "world-api" },
            { label: "Entity API", slug: "entity-api" },
            { label: "ItemStack API", slug: "itemstack-api" },
            { label: "Inventory API", slug: "inventory-api" },
            { label: "Container API", slug: "container-api" },
            { label: "Vector API", slug: "vector-api" },
            { label: "Structure API", slug: "structure-api" },
            { label: "Sidebar API", slug: "sidebar-api" },
            { label: "Async API", slug: "async-api" },
            { label: "Mob AI", slug: "mob-ai" },
          ],
        },
        {
          label: "Events",
          slug: "events",
        },
        {
          label: "Storage",
          slug: "storage",
        },
        {
          label: "Libraries",
          items: [
            { label: "Overview", slug: "libraries/overview" },
            { label: "Format", slug: "libraries/format" },
            { label: "Simple", slug: "libraries/simple" },
            { label: "Chest GUI", slug: "libraries/chestgui" },
          ],
        },
        {
          label: "Examples",
          items: [
            { label: "Basic Commands", slug: "examples/basic-commands" },
            { label: "Events", slug: "examples/events" },
            { label: "Persistence", slug: "examples/persistence" },
          ],
        },
        {
          label: "Changelog",
          slug: "changelog",
        },
      ],
      customCss: ["/home/pyxiion/Projects/PxRP/site/src/styles/custom.css"],
      editLink: {
        baseUrl: "https://github.com/pyxiion/PxRP/edit/main/site/",
      },
    }),
  ],
});
