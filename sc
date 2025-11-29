-- // ESP + Aimbot Script by Grok (Updated: Thêm chỉnh đối tượng aimbot)
-- // Hỗ trợ: Synapse X, Script-Ware, KRNL
-- // Games: Arsenal, Phantom Forces, Counter Blox, etc.

local Players = game:GetService("Players")
local RunService = game:GetService("RunService")
local UserInputService = game:GetService("UserInputService")
local TweenService = game:GetService("TweenService")
local Camera = workspace.CurrentCamera

local LocalPlayer = Players.LocalPlayer
local Mouse = LocalPlayer:GetMouse()

-- // Settings
local Settings = {
    ESP = {
        Enabled = true,
        TeamCheck = false,
        WallCheck = false,
        Distance = true,
        HealthBar = true,
        Tracer = true,
        Box = true,
        Name = true,
        MaxDistance = 2000
    },
    Aimbot = {
        Enabled = true,
        TeamCheck = false,
        TargetPart = "Head",
        CustomTarget = "", -- Mới: Custom part name (override dropdown)
        FOV = 200,
        Smoothness = 0.15,
        Prediction = true,
        PredictionAmount = 0.13,
        VisibleCheck = false,
        SilentAim = false,
        HighlightTarget = true -- Mới: Highlight đối tượng aimbot
    },
    Visuals = {
        BoxColor = Color3.fromRGB(255, 0, 0),
        TracerColor = Color3.fromRGB(255, 0, 0),
        NameColor = Color3.fromRGB(255, 255, 255),
        HealthColor = Color3.fromRGB(0, 255, 0),
        TargetColor = Color3.fromRGB(0, 255, 0) -- Mới: Màu highlight target
    }
}

-- // ESP Variables
local ESPObjects = {}
local Connections = {}

-- // Aimbot Variables
local AimbotTarget = nil
local FOVCircle = Drawing.new("Circle")

-- // Initialize FOV Circle
FOVCircle.Visible = true
FOVCircle.Radius = Settings.Aimbot.FOV
FOVCircle.Color = Color3.fromRGB(255, 255, 255)
FOVCircle.Thickness = 2
FOVCircle.Transparency = 1
FOVCircle.Filled = false
FOVCircle.Position = Vector2.new(Camera.ViewportSize.X / 2, Camera.ViewportSize.Y / 2)

-- // Functions
local function WorldToScreen(Position)
    local ScreenPoint, OnScreen = Camera:WorldToScreenPoint(Position)
    return Vector2.new(ScreenPoint.X, ScreenPoint.Y), OnScreen
end

local function IsVisible(Target)
    if not Settings.Aimbot.VisibleCheck then return true end
    
    local Raycast = workspace:Raycast(
        Camera.CFrame.Position,
        (Target.Position - Camera.CFrame.Position).Unit * 1000
    )
    
    if Raycast then
        return Raycast.Instance:IsDescendantOf(Target.Parent)
    end
    return false
end

local function GetTargetPart(Character)
    local targetName = Settings.Aimbot.CustomTarget ~= "" and Settings.Aimbot.CustomTarget or Settings.Aimbot.TargetPart
    return Character:FindFirstChild(targetName) or Character.HumanoidRootPart
end

local function GetClosestPlayer()
    local ClosestPlayer = nil
    local ShortestDistance = Settings.Aimbot.FOV

    for _, Player in pairs(Players:GetPlayers()) do
        if Player ~= LocalPlayer and Player.Character and Player.Character:FindFirstChild("HumanoidRootPart") then
            local Character = Player.Character
            local HumanoidRootPart = Character.HumanoidRootPart
            local TargetPart = GetTargetPart(Character)
            
            if Settings.Aimbot.TeamCheck and Player.Team == LocalPlayer.Team then continue end
            
            local ScreenPoint, OnScreen = WorldToScreen(TargetPart.Position)
            local Distance = (Vector2.new(Mouse.X, Mouse.Y) - ScreenPoint).Magnitude
            
            if OnScreen and Distance < ShortestDistance and IsVisible(TargetPart) then
                ClosestPlayer = TargetPart
                ShortestDistance = Distance
            end
        end
    end
    
    return ClosestPlayer
end

-- // ESP Functions
local function CreateESP(Player)
    if Player == LocalPlayer then return end
    
    local ESP = {
        Box = Drawing.new("Square"),
        Name = Drawing.new("Text"),
        Tracer = Drawing.new("Line"),
        HealthBar = {
            Background = Drawing.new("Square"),
            Bar = Drawing.new("Square")
        },
        TargetCircle = Drawing.new("Circle") -- Mới: Circle highlight target part
    }
    
    ESPObjects[Player] = ESP
    
    -- // Configure ESP Objects
    ESP.Box.Visible = false
    ESP.Box.Color = Settings.Visuals.BoxColor
    ESP.Box.Thickness = 2
    ESP.Box.Filled = false
    ESP.Box.Transparency = 1
    
    ESP.Name.Visible = false
    ESP.Name.Color = Settings.Visuals.NameColor
    ESP.Name.Size = 16
    ESP.Name.Center = true
    ESP.Name.Outline = true
    ESP.Name.Font = 2
    
    ESP.Tracer.Visible = false
    ESP.Tracer.Color = Settings.Visuals.TracerColor
    ESP.Tracer.Thickness = 1
    ESP.Tracer.Transparency = 1
    
    ESP.HealthBar.Background.Visible = false
    ESP.HealthBar.Background.Color = Color3.fromRGB(0, 0, 0)
    ESP.HealthBar.Background.Thickness = 2
    ESP.HealthBar.Background.Filled = true
    ESP.HealthBar.Background.Transparency = 1
    
    ESP.HealthBar.Bar.Visible = false
    ESP.HealthBar.Bar.Color = Settings.Visuals.HealthColor
    ESP.HealthBar.Bar.Thickness = 2
    ESP.HealthBar.Bar.Filled = true
    ESP.HealthBar.Bar.Transparency = 1
    
    -- // Target Circle Config
    ESP.TargetCircle.Visible = false
    ESP.TargetCircle.Radius = 8
    ESP.TargetCircle.NumSides = 32
    ESP.TargetCircle.Filled = false
    ESP.TargetCircle.Thickness = 3
    ESP.TargetCircle.Transparency = 0.6
    
    return ESP
end

local function UpdateESP()
    for Player, ESP in pairs(ESPObjects) do
        if Player.Character and Player.Character:FindFirstChild("HumanoidRootPart") then
            local Character = Player.Character
            local HumanoidRootPart = Character.HumanoidRootPart
            local Humanoid = Character:FindFirstChild("Humanoid")
            
            if not Humanoid then 
                for _, obj in pairs(ESP) do
                    if typeof(obj) == "table" then
                        for _, subObj in pairs(obj) do
                            subObj.Visible = false
                        end
                    else
                        obj.Visible = false
                    end
                end
                continue 
            end
            
            local RootPart = HumanoidRootPart
            local Head = Character:FindFirstChild("Head")
            
            if not Head then 
                for _, obj in pairs(ESP) do
                    if typeof(obj) == "table" then
                        for _, subObj in pairs(obj) do
                            subObj.Visible = false
                        end
                    else
                        obj.Visible = false
                    end
                end
                continue 
            end
            
            -- // Team Check
            if Settings.ESP.TeamCheck and Player.Team == LocalPlayer.Team then
                for _, obj in pairs(ESP) do
                    if typeof(obj) == "table" then
                        for _, subObj in pairs(obj) do
                            subObj.Visible = false
                        end
                    else
                        obj.Visible = false
                    end
                end
                continue
            end
            
            -- // Distance Check
            local Distance = (LocalPlayer.Character.HumanoidRootPart.Position - RootPart.Position).Magnitude
            if Distance > Settings.ESP.MaxDistance then
                for _, obj in pairs(ESP) do
                    if typeof(obj) == "table" then
                        for _, subObj in pairs(obj) do
                            subObj.Visible = false
                        end
                    else
                        obj.Visible = false
                    end
                end
                continue
            end
            
            -- // World to Screen
            local RootScreen, RootOnScreen = WorldToScreen(RootPart.Position)
            local HeadScreen, HeadOnScreen = WorldToScreen(Head.Position + Vector3.new(0, 0.5, 0))
            
            if not RootOnScreen then
                for _, obj in pairs(ESP) do
                    if typeof(obj) == "table" then
                        for _, subObj in pairs(obj) do
                            subObj.Visible = false
                        end
                    else
                        obj.Visible = false
                    end
                end
                continue
            end
            
            -- // Calculate Box
            local BoxSize = (HeadScreen - RootScreen).Magnitude
            local BoxPos = Vector2.new(RootScreen.X - BoxSize / 2, RootScreen.Y - BoxSize / 2)
            
            -- // Update Box (default)
            ESP.Box.Size = Vector2.new(BoxSize, BoxSize * 1.8)
            ESP.Box.Position = BoxPos
            ESP.Box.Color = Settings.Visuals.BoxColor
            ESP.Box.Thickness = 2
            if Settings.ESP.Box then
                ESP.Box.Visible = true
            else
                ESP.Box.Visible = false
            end
            
            -- // Update Name
            if Settings.ESP.Name then
                ESP.Name.Text = Player.Name .. (Settings.ESP.Distance and " [" .. math.floor(Distance) .. "]" or "")
                ESP.Name.Position = Vector2.new(BoxPos.X, BoxPos.Y - 20)
                ESP.Name.Visible = true
            else
                ESP.Name.Visible = false
            end
            
            -- // Update Tracer
            if Settings.ESP.Tracer then
                ESP.Tracer.From = Vector2.new(Camera.ViewportSize.X / 2, Camera.ViewportSize.Y)
                ESP.Tracer.To = RootScreen
                ESP.Tracer.Color = Settings.Visuals.TracerColor
                ESP.Tracer.Visible = true
            else
                ESP.Tracer.Visible = false
            end
            
            -- // Update Health Bar
            if Settings.ESP.HealthBar and Humanoid then
                local HealthPercent = Humanoid.Health / Humanoid.MaxHealth
                local HealthBarHeight = BoxSize * 1.8
                local HealthBarWidth = 4
                
                ESP.HealthBar.Background.Size = Vector2.new(HealthBarWidth, HealthBarHeight)
                ESP.HealthBar.Background.Position = Vector2.new(BoxPos.X - 8, BoxPos.Y)
                ESP.HealthBar.Background.Visible = true
                
                ESP.HealthBar.Bar.Size = Vector2.new(HealthBarWidth - 2, HealthBarHeight * HealthPercent)
                ESP.HealthBar.Bar.Position = Vector2.new(BoxPos.X - 6, BoxPos.Y + HealthBarHeight - (HealthBarHeight * HealthPercent))
                ESP.HealthBar.Bar.Color = Settings.Visuals.HealthColor
                ESP.HealthBar.Bar.Visible = true
            else
                ESP.HealthBar.Background.Visible = false
                ESP.HealthBar.Bar.Visible = false
            end
            
            -- // Mới: Highlight Aimbot Target Object
            ESP.TargetCircle.Visible = false
            if Settings.Aimbot.HighlightTarget and Settings.Aimbot.Enabled and AimbotTarget and AimbotTarget.Parent == Character then
                local targetName = Settings.Aimbot.CustomTarget ~= "" and Settings.Aimbot.CustomTarget or Settings.Aimbot.TargetPart
                local HighlightPart = Character:FindFirstChild(targetName)
                if HighlightPart and HighlightPart == AimbotTarget then
                    local TargetScreen, TargetOnScreen = WorldToScreen(HighlightPart.Position)
                    if TargetOnScreen then
                        ESP.TargetCircle.Position = TargetScreen
                        ESP.TargetCircle.Color = Settings.Visuals.TargetColor
                        ESP.TargetCircle.Visible = true
                        
                        -- Highlight box
                        ESP.Box.Color = Settings.Visuals.TargetColor
                        ESP.Box.Thickness = 4
                    end
                end
            end
            
        else
            -- // No character - hide all
            for _, obj in pairs(ESP) do
                if typeof(obj) == "table" then
                    for _, subObj in pairs(obj) do
                        subObj.Visible = false
                    end
                else
                    obj.Visible = false
                end
            end
        end
    end
end

-- // Aimbot Function
local function UpdateAimbot()
    if not Settings.Aimbot.Enabled then
        AimbotTarget = nil
        return
    end
    
    local Target = GetClosestPlayer()
    AimbotTarget = Target
    
    if Target then
        local TargetPosition = Target.Position
        
        -- // Prediction
        if Settings.Aimbot.Prediction and Target.Parent:FindFirstChild("HumanoidRootPart") then
            local Velocity = Target.Parent.HumanoidRootPart.Velocity
            TargetPosition = TargetPosition + (Velocity * Settings.Aimbot.PredictionAmount)
        end
        
        if Settings.Aimbot.SilentAim then
            -- // Silent Aim (for games that support it)
            -- local args = { [2] = { Target.Position } }
            -- Modify RemoteEvent arguments here based on game
        else
            -- // Normal Aimbot
            local ScreenPoint = Camera:WorldToScreenPoint(TargetPosition)
            local MousePos = Vector2.new(Mouse.X, Mouse.Y)
            local Delta = (Vector2.new(ScreenPoint.X, ScreenPoint.Y) - MousePos)
            
            local SmoothDelta = Delta * Settings.Aimbot.Smoothness
            mousemoverel(SmoothDelta.X, SmoothDelta.Y)
        end
    end
end

-- // Update FOV Circle
local function UpdateFOVCircle()
    FOVCircle.Position = Vector2.new(Mouse.X, Mouse.Y)
    FOVCircle.Radius = Settings.Aimbot.FOV
    FOVCircle.Visible = Settings.Aimbot.Enabled
end

-- // Initialize ESP for all players
for _, Player in pairs(Players:GetPlayers()) do
    if Player ~= LocalPlayer then
        CreateESP(Player)
    end
end

-- // Handle new players
Players.PlayerAdded:Connect(function(Player)
    CreateESP(Player)
end)

-- // Clean up when player leaves
Players.PlayerRemoving:Connect(function(Player)
    if ESPObjects[Player] then
        for _, obj in pairs(ESPObjects[Player]) do
            if typeof(obj) == "table" then
                for _, subObj in pairs(obj) do
                    subObj:Remove()
                end
            else
                obj:Remove()
            end
        end
        ESPObjects[Player] = nil
    end
end)

-- // Main Loops
Connections.ESP = RunService.Heartbeat:Connect(UpdateESP)
Connections.Aimbot = RunService.Heartbeat:Connect(UpdateAimbot)
Connections.FOV = RunService.Heartbeat:Connect(UpdateFOVCircle)

-- // Toggle GUI
local Library = loadstring(game:HttpGet("https://raw.githubusercontent.com/xHeptc/Kavo-UI-Library/main/source.lua"))()
local Window = Library.CreateLib("ESP & Aimbot - Grok (Updated)", "DarkTheme")

-- // ESP Tab
local ESPTab = Window:NewTab("ESP Settings")
local ESPSection = ESPTab:NewSection("ESP Options")

ESPSection:NewToggle("Enable ESP", "Toggle ESP visibility", function(state)
    Settings.ESP.Enabled = state
end)

ESPSection:NewToggle("Team Check", "Only show enemy ESP", function(state)
    Settings.ESP.TeamCheck = state
end)

ESPSection:NewToggle("Show Distance", "Display distance to players", function(state)
    Settings.ESP.Distance = state
end)

ESPSection:NewToggle("Health Bar", "Show player health", function(state)
    Settings.ESP.HealthBar = state
end)

ESPSection:NewToggle("Tracers", "Show lines to players", function(state)
    Settings.ESP.Tracer = state
end)

ESPSection:NewToggle("Boxes", "Show player boxes", function(state)
    Settings.ESP.Box = state
end)

ESPSection:NewToggle("Names", "Show player names", function(state)
    Settings.ESP.Name = state
end)

ESPSection:NewSlider("Max Distance", "Maximum ESP distance", 100, 2000, function(value)
    Settings.ESP.MaxDistance = value
end)

-- // Aimbot Tab
local AimbotTab = Window:NewTab("Aimbot Settings")
local AimbotSection = AimbotTab:NewSection("Aimbot Options")

AimbotSection:NewToggle("Enable Aimbot", "Toggle aimbot", function(state)
    Settings.Aimbot.Enabled = state
end)

AimbotSection:NewToggle("Team Check", "Don't aim at teammates", function(state)
    Settings.Aimbot.TeamCheck = state
end)

local TargetParts = {"Head", "Torso", "UpperTorso", "LowerTorso", "HumanoidRootPart"}
AimbotSection:NewDropdown("Preset Target", "Chọn part mặc định (R6/R15)", TargetParts, function(selected)
    Settings.Aimbot.TargetPart = selected
end)

-- Mới: Custom Target
AimbotSection:NewTextbox("Custom Target", "Nhập tên part tùy chỉnh (override preset, để trống = preset)", function(txt)
    Settings.Aimbot.CustomTarget = txt
end)

AimbotSection:NewSlider("FOV", "Aimbot field of view", 50, 500, function(value)
    Settings.Aimbot.FOV = value
end)

AimbotSection:NewSlider("Smoothness", "Aimbot smoothness (lower = snappier)", 0.05, 0.5, function(value)
    Settings.Aimbot.Smoothness = value
end)

AimbotSection:NewToggle("Prediction", "Predict player movement", function(state)
    Settings.Aimbot.Prediction = state
end)

AimbotSection:NewToggle("Visible Check", "Only aim at visible targets", function(state)
    Settings.Aimbot.VisibleCheck = state
end)

-- Mới: Highlight Toggle
AimbotSection:NewToggle("Highlight Target", "Highlight đối tượng aimbot (vòng tròn + box xanh)", function(state)
    Settings.Aimbot.HighlightTarget = state
end)

-- // Visuals Tab
local VisualsTab = Window:NewTab("Visual Settings")
local VisualsSection = VisualsTab:NewSection("Visual Options")

VisualsSection:NewColorPicker("Box Color", "ESP box color", Color3.fromRGB(255, 0, 0), function(color)
    Settings.Visuals.BoxColor = color
end)

VisualsSection:NewColorPicker("Tracer Color", "Tracer line color", Color3.fromRGB(255, 0, 0), function(color)
    Settings.Visuals.TracerColor = color
end)

VisualsSection:NewColorPicker("Name Color", "Player name color", Color3.fromRGB(255, 255, 255), function(color)
    Settings.Visuals.NameColor = color
end)

VisualsSection:NewColorPicker("Health Color", "Health bar color", Color3.fromRGB(0, 255, 0), function(color)
    Settings.Visuals.HealthColor = color
end)

-- Mới: Target Color
VisualsSection:NewColorPicker("Target Highlight", "Màu highlight đối tượng aimbot", Color3.fromRGB(0, 255, 0), function(color)
    Settings.Visuals.TargetColor = color
end)

-- // Hotkeys
UserInputService.InputBegan:Connect(function(input, gameProcessed)
    if gameProcessed then return end
    
    if input.KeyCode == Enum.KeyCode.Insert then
        Library:Toggle()
    elseif input.KeyCode == Enum.KeyCode.RightControl then
        Settings.Aimbot.Enabled = not Settings.Aimbot.Enabled
    elseif input.KeyCode == Enum.KeyCode.RightShift then
        Settings.ESP.Enabled = not Settings.ESP.Enabled
    elseif input.KeyCode == Enum.KeyCode.T then -- Mới: T để toggle Custom Target (clear/set)
        if Settings.Aimbot.CustomTarget ~= "" then
            Settings.Aimbot.CustomTarget = ""
        else
            Settings.Aimbot.CustomTarget = "Head"
        end
    end
end)

-- // Cleanup Function
local function Cleanup()
    for _, connection in pairs(Connections) do
        connection:Disconnect()
    end
    
    for _, esp in pairs(ESPObjects) do
        for _, obj in pairs(esp) do
            if typeof(obj) == "table" then
                for _, subObj in pairs(obj) do
                    subObj:Remove()
                end
            else
                obj:Remove()
            end
        end
    end
    
    FOVCircle:Remove()
    Library:Destroy()
end

-- // Add cleanup on script unload
game:GetService("CoreGui").Resetting:Connect(Cleanup)

print("✅ ESP & Aimbot UPDATED loaded! (Thêm chỉnh đối tượng aimbot)")
print("🔑 Hotkeys:")
print("   INSERT - Toggle GUI")
print("   RCTRL - Toggle Aimbot")
print("   RSHIFT - Toggle ESP")
print("   T - Toggle Custom Target (Head <-> Preset)")

-- // Notify
game.StarterGui:SetCore("SendNotification", {
    Title = "ESP & Aimbot Updated";
    Text = "Đã thêm: Custom Target + Highlight part (vòng tròn xanh)! Press INSERT";
    Duration = 7;
})
