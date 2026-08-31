export function addMachineToMineSite($event, widgetContext, additionalParams) {
    let $injector = widgetContext.$scope.$injector;
    let customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
    let deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));
    let entityGroupService = $injector.get(widgetContext.servicesMap.get('entityGroupService'));
    let attributeService = $injector.get(widgetContext.servicesMap.get('attributeService'));
    let entityRelationService = $injector.get(widgetContext.servicesMap.get('entityRelationService'));

    openAddMachineDialog();

    function openAddMachineDialog() {

        const addMachineHtmlTemplate = '<form #addEntityForm="ngForm" [formGroup]="addMachineFormGroup"\n' +
                                       '      (ngSubmit)="save()" class="add-entity-form" style="width: 600px">\n' +
                                       '  <mat-toolbar class="flex flex-row" color="primary">\n' +
                                       '    <h2>Add new machine</h2>\n' +
                                       '    <span class="flex-1"></span>\n' +
                                       '    <button mat-icon-button (click)="cancel()" type="button">\n' +
                                       '      <mat-icon class="material-icons">close</mat-icon>\n' +
                                       '    </button>\n' +
                                       '  </mat-toolbar>\n' +
                                       '  <mat-progress-bar color="warn" mode="indeterminate" *ngIf="isLoading$ | async">\n' +
                                       '  </mat-progress-bar>\n' +
                                       '  <div style="height: 4px;" *ngIf="!(isLoading$ | async)"></div>\n' +
                                       '  <div mat-dialog-content class="flex flex-col">\n' +
                                       '    <mat-form-field class="mat-block" appearance="outline">\n' +
                                       '      <mat-label>Machine name</mat-label>\n' +
                                       '      <input matInput formControlName="machineName" required>\n' +
                                       '      <mat-error *ngIf="addMachineFormGroup.get(\'machineName\').hasError(\'required\')">\n' +
                                       '        Machine name is required.\n' +
                                       '      </mat-error>\n' +
                                       '    </mat-form-field>\n' +
                                       '    <mat-form-field class="mat-block" appearance="outline">\n' +
                                       '      <mat-label>Machine type</mat-label>\n' +
                                       '      <mat-select formControlName="machineType" required>\n' +
                                       '        <mat-option *ngFor="let type of machineTypes" [value]="type.value">\n' +
                                       '          {{type.label}}\n' +
                                       '        </mat-option>\n' +
                                       '      </mat-select>\n' +
                                       '      <mat-error *ngIf="addMachineFormGroup.get(\'machineType\').hasError(\'required\')">\n' +
                                       '        Machine type is required.\n' +
                                       '      </mat-error>\n' +
                                       '    </mat-form-field>\n' +
                                       '    <div class="flex flex-row gap-2" *ngIf="addMachineFormGroup.get(\'machineType\').value === \'Excavator\'">\n' +
                                       '      <mat-form-field class="flex-1" appearance="outline">\n' +
                                       '        <mat-label>Warning pressure limit</mat-label>\n' +
                                       '        <input matInput type="number" formControlName="warningThreshold" required>\n' +
                                       '        <span style="margin-right: 8px" matSuffix>{{pressureUnitSymbol}}</span>\n' +
                                       '        <mat-error *ngIf="addMachineFormGroup.get(\'warningThreshold\').hasError(\'required\')">\n' +
                                       '          Required.\n' +
                                       '        </mat-error>\n' +
                                       '      </mat-form-field>\n' +
                                       '      <mat-form-field class="flex-1" appearance="outline">\n' +
                                       '        <mat-label>Critical pressure limit</mat-label>\n' +
                                       '        <input matInput type="number" formControlName="criticalThreshold" required>\n' +
                                       '        <span style="margin-right: 8px" matSuffix>{{pressureUnitSymbol}}</span>\n' +
                                       '        <mat-error *ngIf="addMachineFormGroup.get(\'criticalThreshold\').hasError(\'required\')">\n' +
                                       '          Required.\n' +
                                       '        </mat-error>\n' +
                                       '      </mat-form-field>\n' +
                                       '    </div>\n' +
                                       '    <div class="flex flex-row gap-2" *ngIf="addMachineFormGroup.get(\'machineType\').value === \'Truck\'">\n' +
                                       '      <mat-form-field class="flex-1" appearance="outline">\n' +
                                       '        <mat-label>Warning weight limit</mat-label>\n' +
                                       '        <input matInput type="number" formControlName="warningThreshold" required>\n' +
                                       '        <span style="margin-right: 8px" matSuffix>{{weightUnitSymbol}}</span>\n' +
                                       '        <mat-error *ngIf="addMachineFormGroup.get(\'warningThreshold\').hasError(\'required\')">\n' +
                                       '          Required.\n' +
                                       '        </mat-error>\n' +
                                       '      </mat-form-field>\n' +
                                       '      <mat-form-field class="flex-1" appearance="outline">\n' +
                                       '        <mat-label>Critical weight limit</mat-label>\n' +
                                       '        <input matInput type="number" formControlName="criticalThreshold" required>\n' +
                                       '        <span style="margin-right: 8px" matSuffix>{{weightUnitSymbol}}</span>\n' +
                                       '        <mat-error *ngIf="addMachineFormGroup.get(\'criticalThreshold\').hasError(\'required\')">\n' +
                                       '          Required.\n' +
                                       '        </mat-error>\n' +
                                       '      </mat-form-field>\n' +
                                       '    </div>\n' +
                                       '  </div>\n' +
                                       '  <div mat-dialog-actions class="flex flex-row items-center justify-end">\n' +
                                       '    <button mat-button color="primary"\n' +
                                       '            type="button"\n' +
                                       '            [disabled]="(isLoading$ | async)"\n' +
                                       '            (click)="cancel()" cdkFocusInitial>\n' +
                                       '      Cancel\n' +
                                       '    </button>\n' +
                                       '    <button mat-button mat-raised-button color="primary"\n' +
                                       '            type="submit"\n' +
                                       '            [disabled]="(isLoading$ | async) || addMachineFormGroup.invalid || !addMachineFormGroup.dirty">\n' +
                                       '      Add\n' +
                                       '    </button>\n' +
                                       '  </div>\n' +
                                       '</form>';

        customDialog.customDialog(addMachineHtmlTemplate, AddMachineDialogController).subscribe();
    }

    function AddMachineDialogController(instance) {
        let vm = instance;

        const pressureUnit = {
            from: 'psi',
            METRIC: 'bar',
            IMPERIAL: 'psi',
            HYBRID: 'bar'
        };

        const loadWeightUnit = {
            from: 'lb',
            METRIC: 't',
            IMPERIAL: 'lb',
            HYBRID: 't'
        };

        vm.pressureUnitSymbol = widgetContext.unitService.getTargetUnitSymbol(pressureUnit);
        vm.weightUnitSymbol = widgetContext.unitService.getTargetUnitSymbol(loadWeightUnit);

        const pressureFromMetric = {
            from: 'bar',
            METRIC: 'bar',
            IMPERIAL: 'psi',
            HYBRID: 'bar'
        };

        const weightFromMetric = {
            from: 't',
            METRIC: 't',
            IMPERIAL: 'lb',
            HYBRID: 't'
        };

        const defaultPressureWarning = Math.round(widgetContext.unitService.convertUnitValue(5100, pressureFromMetric));
        const defaultPressureCritical = Math.round(widgetContext.unitService.convertUnitValue(5500, pressureFromMetric));
        const defaultWeightWarning = Math.round(widgetContext.unitService.convertUnitValue(32, weightFromMetric));
        const defaultWeightCritical = Math.round(widgetContext.unitService.convertUnitValue(35, weightFromMetric));

        vm.machineTypes = [
            { value: 'Excavator', label: 'Excavator' },
            { value: 'Truck', label: 'Truck' }
        ];

        vm.addMachineFormGroup = vm.fb.group({
            machineName: ['', [vm.validators.required]],
            machineType: [null, [vm.validators.required]],
            warningThreshold: [null, [vm.validators.required]],
            criticalThreshold: [null, [vm.validators.required]]
        });

        vm.addMachineFormGroup.get('machineType').valueChanges.subscribe((type) => {
            if (type === 'Excavator') {
                vm.addMachineFormGroup.patchValue({
                    warningThreshold: defaultPressureWarning,
                    criticalThreshold: defaultPressureCritical
                });
            } else if (type === 'Truck') {
                vm.addMachineFormGroup.patchValue({
                    warningThreshold: defaultWeightWarning,
                    criticalThreshold: defaultWeightCritical
                });
            }
        });

        vm.cancel = function() {
            vm.dialogRef.close(null);
        };

        vm.save = function() {
            vm.addMachineFormGroup.markAsPristine();
            saveDeviceObservable().subscribe(
                function(device) {
                    getMineSiteId().pipe(
                        widgetContext.rxjs.switchMap((mineSiteId) => {
                            return widgetContext.rxjs.forkJoin([
                                saveMineSiteToMachineRelation(mineSiteId, device.id),
                                saveAttributes(device.id)
                            ]);
                        })
                    ).subscribe(
                        function() {
                            widgetContext.updateAliases();
                            vm.dialogRef.close(null);
                        }
                    );
                }
            );
        };

        function saveDeviceObservable() {
            return getOrCreateMachinesDeviceGroup().pipe(
                widgetContext.rxjs.switchMap((machinesGroup) => {
                    const formValues = vm.addMachineFormGroup.value;
                    const deviceProfile = formValues.machineType === 'Truck' ? 'Haul truck' : 'Excavator';
                    let machine = {
                        name: formValues.machineName,
                        label: formValues.machineName,
                        type: deviceProfile
                    };
                    return deviceService.saveDevice(machine, machinesGroup.id.id);
                })
            );
        }

        function getOrCreateMachinesDeviceGroup() {
            return getEntityGroupByName('Machines', 'DEVICE').pipe(
                widgetContext.rxjs.switchMap((group) => {
                    if (group) {
                        return widgetContext.rxjs.of(group);
                    } else {
                        let machinesGroup = {
                            type: 'DEVICE',
                            name: 'Machines'
                        };
                        return entityGroupService.saveEntityGroup(machinesGroup);
                    }
                })
            );
        }

        function getEntityGroupByName(groupName, groupType) {
            let entityGroupsPageLink = widgetContext.pageLink(10, 0, groupName);
            return entityGroupService.getEntityGroupsByPageLink(entityGroupsPageLink, groupType, { ignoreLoading: true }).pipe(
                widgetContext.rxjs.map((data) => {
                    if (data.data.length) {
                        return data.data.find((group) => group.name === groupName);
                    } else {
                        return null;
                    }
                })
            );
        }

        function getMineSiteId() {
            const mineSiteAliasId = widgetContext.aliasController.getEntityAliasId('Mine site');
            return widgetContext.aliasController.getAliasInfo(mineSiteAliasId).pipe(
                widgetContext.rxjs.map((info) => {
                    return { id: info.currentEntity.id, entityType: info.currentEntity.entityType };
                })
            );
        }

        function saveMineSiteToMachineRelation(mineSiteId, machineId) {
            let relation = {
                from: mineSiteId,
                to: machineId,
                typeGroup: 'COMMON',
                type: 'MineSiteToMachine'
            };
            return entityRelationService.saveRelation(relation);
        }

        function saveAttributes(entityId) {
            const formValues = vm.addMachineFormGroup.value;
            const machineType = formValues.machineType;
            const warningValue = formValues.warningThreshold;
            const criticalValue = formValues.criticalThreshold;

            let attributesArray = [];

            if (machineType === 'Excavator') {
                const pressureToStorage = {
                    from: vm.pressureUnitSymbol,
                    METRIC: 'psi',
                    IMPERIAL: 'psi',
                    HYBRID: 'psi'
                };
                const warningInPsi = widgetContext.unitService.convertUnitValue(warningValue, pressureToStorage);
                const criticalInPsi = widgetContext.unitService.convertUnitValue(criticalValue, pressureToStorage);

                attributesArray.push({ key: 'hydraulicPressureWarningThreshold', value: warningInPsi });
                attributesArray.push({ key: 'hydraulicPressureCriticalThreshold', value: criticalInPsi });
            } else if (machineType === 'Truck') {
                const weightToStorage = {
                    from: vm.weightUnitSymbol,
                    METRIC: 'lb',
                    IMPERIAL: 'lb',
                    HYBRID: 'lb'
                };
                const warningInLb = widgetContext.unitService.convertUnitValue(warningValue, weightToStorage);
                const criticalInLb = widgetContext.unitService.convertUnitValue(criticalValue, weightToStorage);

                attributesArray.push({ key: 'loadWeightWarningThreshold', value: warningInLb });
                attributesArray.push({ key: 'loadWeightCriticalThreshold', value: criticalInLb });
            }

            let timeseriesArray = getMapItemLocationTimeseries();

            let tasks = [];

            if (timeseriesArray.length > 0) {
                tasks.push(attributeService.saveEntityTimeseries(entityId, 'LATEST_TELEMETRY', timeseriesArray));
            }

            if (tasks.length > 0) {
                return widgetContext.rxjs.forkJoin(tasks);
            }
            return widgetContext.rxjs.of([]);
        }

        function getMapItemLocationTimeseries() {
            const timeseries = [];
            const mapItemType = $event.shape;
            if (mapItemType === 'Marker') {
                const mapType = widgetContext.mapInstance.type();
                if (mapType !== 'image') {
                    timeseries.push({ key: 'latitude', value: additionalParams.coordinates.x });
                    timeseries.push({ key: 'longitude', value: additionalParams.coordinates.y });
                }
            }
            return timeseries;
        }
    }
}

export function addZoneToMineSite($event, widgetContext, additionalParams) {
    let $injector = widgetContext.$scope.$injector;
    let customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
    let assetService = $injector.get(widgetContext.servicesMap.get('assetService'));
    let entityGroupService = $injector.get(widgetContext.servicesMap.get('entityGroupService'));
    let attributeService = $injector.get(widgetContext.servicesMap.get('attributeService'));
    let entityRelationService = $injector.get(widgetContext.servicesMap.get('entityRelationService'));

    openAddZoneDialog();

    function openAddZoneDialog() {

        const addZoneHtmlTemplate = '<form #addEntityForm="ngForm" [formGroup]="addZoneFormGroup"\n' +
                                    '      (ngSubmit)="save()" class="add-entity-form" style="width: 600px;">\n' +
                                    '  <mat-toolbar class="flex flex-row" color="primary">\n' +
                                    '    <h2>Add new zone</h2>\n' +
                                    '    <span class="flex-1"></span>\n' +
                                    '    <button mat-icon-button (click)="cancel()" type="button">\n' +
                                    '      <mat-icon class="material-icons">close</mat-icon>\n' +
                                    '    </button>\n' +
                                    '  </mat-toolbar>\n' +
                                    '  <mat-progress-bar color="warn" mode="indeterminate" *ngIf="isLoading$ | async">\n' +
                                    '  </mat-progress-bar>\n' +
                                    '  <div style="height: 4px;" *ngIf="!(isLoading$ | async)"></div>\n' +
                                    '  <div mat-dialog-content class="flex flex-col">\n' +
                                    '    <mat-form-field class="mat-block" appearance="outline">\n' +
                                    '      <mat-label>Zone name</mat-label>\n' +
                                    '      <input matInput formControlName="zoneName" required>\n' +
                                    '      <mat-error *ngIf="addZoneFormGroup.get(\'zoneName\').hasError(\'required\')">\n' +
                                    '        Zone name is required.\n' +
                                    '      </mat-error>\n' +
                                    '    </mat-form-field>\n' +
                                    '    <mat-form-field class="mat-block" appearance="outline">\n' +
                                    '      <mat-label>Zone type</mat-label>\n' +
                                    '      <mat-select formControlName="zoneType" required>\n' +
                                    '        <mat-option *ngFor="let type of zoneTypes" [value]="type.value">\n' +
                                    '          {{type.label}}\n' +
                                    '        </mat-option>\n' +
                                    '      </mat-select>\n' +
                                    '      <mat-error *ngIf="addZoneFormGroup.get(\'zoneType\').hasError(\'required\')">\n' +
                                    '        Zone type is required.\n' +
                                    '      </mat-error>\n' +
                                    '    </mat-form-field>\n' +
                                    '  </div>\n' +
                                    '  <div mat-dialog-actions class="flex flex-row items-center justify-end">\n' +
                                    '    <button mat-button color="primary"\n' +
                                    '            type="button"\n' +
                                    '            [disabled]="(isLoading$ | async)"\n' +
                                    '            (click)="cancel()" cdkFocusInitial>\n' +
                                    '      Cancel\n' +
                                    '    </button>\n' +
                                    '    <button mat-button mat-raised-button color="primary"\n' +
                                    '            type="submit"\n' +
                                    '            [disabled]="(isLoading$ | async) || addZoneFormGroup.invalid || !addZoneFormGroup.dirty">\n' +
                                    '      Add\n' +
                                    '    </button>\n' +
                                    '  </div>\n' +
                                    '</form>';

        customDialog.customDialog(addZoneHtmlTemplate, AddZoneDialogController).subscribe();
    }

    function AddZoneDialogController(instance) {
        let vm = instance;

        vm.zoneTypes = [
            { value: 'Loading', label: 'Loading' },
            { value: 'Unloading', label: 'Unloading' },
            { value: 'Restricted', label: 'Restricted' }
        ];

        vm.addZoneFormGroup = vm.fb.group({
            zoneName: ['', [vm.validators.required]],
            zoneType: [null, [vm.validators.required]]
        });

        vm.cancel = function() {
            vm.dialogRef.close(null);
        };

        vm.save = function() {
            vm.addZoneFormGroup.markAsPristine();
            saveAssetObservable().subscribe(
                function(asset) {
                    getMineSiteId().pipe(
                        widgetContext.rxjs.switchMap((mineSiteId) => {
                            return widgetContext.rxjs.forkJoin([
                                saveMineSiteToZoneRelation(mineSiteId, asset.id),
                                saveAttributes(asset.id)
                            ]);
                        })
                    ).subscribe(
                        function() {
                            widgetContext.updateAliases();
                            vm.dialogRef.close(null);
                        }
                    );
                }
            );
        };

        function saveAssetObservable() {
            return getOrCreateMineSiteZonesAssetGroup().pipe(
                widgetContext.rxjs.switchMap((zonesGroup) => {
                    const formValues = vm.addZoneFormGroup.value;
                    let zone = {
                        name: formValues.zoneName,
                        label: formValues.zoneName,
                        type: 'Mine site zone'
                    };
                    return assetService.saveAsset(zone, zonesGroup.id.id);
                })
            );
        }

        function getOrCreateMineSiteZonesAssetGroup() {
            return getEntityGroupByName('MineSiteZones', 'ASSET').pipe(
                widgetContext.rxjs.switchMap((group) => {
                    if (group) {
                        return widgetContext.rxjs.of(group);
                    } else {
                        let zonesGroup = {
                            type: 'ASSET',
                            name: 'MineSiteZones'
                        };
                        return entityGroupService.saveEntityGroup(zonesGroup);
                    }
                })
            );
        }

        function getEntityGroupByName(groupName, groupType) {
            let entityGroupsPageLink = widgetContext.pageLink(10, 0, groupName);
            return entityGroupService.getEntityGroupsByPageLink(entityGroupsPageLink, groupType, { ignoreLoading: true }).pipe(
                widgetContext.rxjs.map((data) => {
                    if (data.data.length) {
                        return data.data.find((group) => group.name === groupName);
                    } else {
                        return null;
                    }
                })
            );
        }

        function getMineSiteId() {
            const mineSiteAliasId = widgetContext.aliasController.getEntityAliasId('Mine site');
            return widgetContext.aliasController.getAliasInfo(mineSiteAliasId).pipe(
                widgetContext.rxjs.map((info) => {
                    return { id: info.currentEntity.id, entityType: info.currentEntity.entityType };
                })
            );
        }

        function saveMineSiteToZoneRelation(mineSiteId, zoneId) {
            const formValues = vm.addZoneFormGroup.value;
            const zoneType = formValues.zoneType;

            let relationType;
            switch (zoneType) {
                case 'Loading':
                    relationType = 'MineSiteToLoadingZone';
                    break;
                case 'Unloading':
                    relationType = 'MineSiteToUnloadingZone';
                    break;
                case 'Restricted':
                    relationType = 'MineSiteToRestrictedZone';
                    break;
                default:
                    relationType = 'MineSiteToZone';
            }

            let relation = {
                from: mineSiteId,
                to: zoneId,
                typeGroup: 'COMMON',
                type: relationType
            };
            return entityRelationService.saveRelation(relation);
        }

        function saveAttributes(entityId) {
            let attributesArray = [];

            const formValues = vm.addZoneFormGroup.value;
            attributesArray.push({ key: 'type', value: formValues.zoneType });

            attributesArray.push(...getMapItemLocationAttributes());

            if (attributesArray.length > 0) {
                return attributeService.saveEntityAttributes(entityId, 'SERVER_SCOPE', attributesArray);
            }
            return widgetContext.rxjs.of([]);
        }

        function getMapItemLocationAttributes() {
            const attributes = [];
            const mapItemType = $event.shape;
            if (mapItemType === 'Rectangle' || mapItemType === 'Polygon') {
                attributes.push({ key: 'perimeter', value: additionalParams.coordinates });
            } else if (mapItemType === 'Circle') {
                attributes.push({ key: 'circle', value: additionalParams.coordinates });
            }
            return attributes;
        }
    }
}

export function editMachine(widgetContext, entityId, entityName) {
    let $injector = widgetContext.$scope.$injector;
    let customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
    let deviceService = $injector.get(widgetContext.servicesMap.get('deviceService'));
    let attributeService = $injector.get(widgetContext.servicesMap.get('attributeService'));

    openEditMachineDialog();

    function openEditMachineDialog() {

        const editMachineHtmlTemplate = '<form #editEntityForm="ngForm" [formGroup]="editMachineFormGroup"\n' +
                                        '      (ngSubmit)="save()" class="edit-entity-form" style="width: 600px;">\n' +
                                        '  <mat-toolbar class="flex flex-row" color="primary">\n' +
                                        '    <h2>Edit machine</h2>\n' +
                                        '    <span class="flex-1"></span>\n' +
                                        '    <button mat-icon-button (click)="cancel()" type="button">\n' +
                                        '      <mat-icon class="material-icons">close</mat-icon>\n' +
                                        '    </button>\n' +
                                        '  </mat-toolbar>\n' +
                                        '  <mat-progress-bar color="warn" mode="indeterminate" *ngIf="isLoading$ | async">\n' +
                                        '  </mat-progress-bar>\n' +
                                        '  <div style="height: 4px;" *ngIf="!(isLoading$ | async)"></div>\n' +
                                        '  <div mat-dialog-content class="flex flex-col">\n' +
                                        '    <mat-form-field class="mat-block" appearance="outline">\n' +
                                        '      <mat-label>Machine name</mat-label>\n' +
                                        '      <input matInput formControlName="machineName" required>\n' +
                                        '      <mat-error *ngIf="editMachineFormGroup.get(\'machineName\').hasError(\'required\')">\n' +
                                        '        Machine name is required.\n' +
                                        '      </mat-error>\n' +
                                        '    </mat-form-field>\n' +
                                        '    <div class="flex flex-row gap-2" *ngIf="machineType === \'Excavator\'">\n' +
                                        '      <mat-form-field class="flex-1" appearance="outline">\n' +
                                        '        <mat-label>Warning pressure limit</mat-label>\n' +
                                        '        <input matInput type="number" formControlName="warningThreshold" required>\n' +
                                        '        <span style="margin-right: 8px" matSuffix>{{pressureUnitSymbol}}</span>\n' +
                                        '        <mat-error *ngIf="editMachineFormGroup.get(\'warningThreshold\').hasError(\'required\')">\n' +
                                        '          Required.\n' +
                                        '        </mat-error>\n' +
                                        '      </mat-form-field>\n' +
                                        '      <mat-form-field class="flex-1" appearance="outline">\n' +
                                        '        <mat-label>Critical pressure limit</mat-label>\n' +
                                        '        <input matInput type="number" formControlName="criticalThreshold" required>\n' +
                                        '        <span style="margin-right: 8px" matSuffix>{{pressureUnitSymbol}}</span>\n' +
                                        '        <mat-error *ngIf="editMachineFormGroup.get(\'criticalThreshold\').hasError(\'required\')">\n' +
                                        '          Required.\n' +
                                        '        </mat-error>\n' +
                                        '      </mat-form-field>\n' +
                                        '    </div>\n' +
                                        '    <div class="flex flex-row gap-2" *ngIf="machineType === \'Truck\'">\n' +
                                        '      <mat-form-field class="flex-1" appearance="outline">\n' +
                                        '        <mat-label>Warning weight limit</mat-label>\n' +
                                        '        <input matInput type="number" formControlName="warningThreshold" required>\n' +
                                        '        <span style="margin-right: 8px" matSuffix>{{weightUnitSymbol}}</span>\n' +
                                        '        <mat-error *ngIf="editMachineFormGroup.get(\'warningThreshold\').hasError(\'required\')">\n' +
                                        '          Required.\n' +
                                        '        </mat-error>\n' +
                                        '      </mat-form-field>\n' +
                                        '      <mat-form-field class="flex-1" appearance="outline">\n' +
                                        '        <mat-label>Critical weight limit</mat-label>\n' +
                                        '        <input matInput type="number" formControlName="criticalThreshold" required>\n' +
                                        '        <span style="margin-right: 8px" matSuffix>{{weightUnitSymbol}}</span>\n' +
                                        '        <mat-error *ngIf="editMachineFormGroup.get(\'criticalThreshold\').hasError(\'required\')">\n' +
                                        '          Required.\n' +
                                        '        </mat-error>\n' +
                                        '      </mat-form-field>\n' +
                                        '    </div>\n' +
                                        '  </div>\n' +
                                        '  <div mat-dialog-actions class="flex flex-row items-center justify-end">\n' +
                                        '    <button mat-button color="primary"\n' +
                                        '            type="button"\n' +
                                        '            [disabled]="(isLoading$ | async)"\n' +
                                        '            (click)="cancel()" cdkFocusInitial>\n' +
                                        '      Cancel\n' +
                                        '    </button>\n' +
                                        '    <button mat-button mat-raised-button color="primary"\n' +
                                        '            type="submit"\n' +
                                        '            [disabled]="(isLoading$ | async) || editMachineFormGroup.invalid || !editMachineFormGroup.dirty">\n' +
                                        '      Save\n' +
                                        '    </button>\n' +
                                        '  </div>\n' +
                                        '</form>';

        customDialog.customDialog(editMachineHtmlTemplate, EditMachineDialogController).subscribe();
    }

    function EditMachineDialogController(instance) {
        let vm = instance;

        const pressureUnit = {
            from: 'psi',
            METRIC: 'bar',
            IMPERIAL: 'psi',
            HYBRID: 'bar'
        };

        const loadWeightUnit = {
            from: 'lb',
            METRIC: 't',
            IMPERIAL: 'lb',
            HYBRID: 't'
        };

        vm.pressureUnitSymbol = widgetContext.unitService.getTargetUnitSymbol(pressureUnit);
        vm.weightUnitSymbol = widgetContext.unitService.getTargetUnitSymbol(loadWeightUnit);

        vm.device = null;
        vm.machineType = null;

        vm.editMachineFormGroup = vm.fb.group({
            machineName: ['', [vm.validators.required]],
            warningThreshold: [null, [vm.validators.required]],
            criticalThreshold: [null, [vm.validators.required]]
        });

        loadMachine();

        vm.cancel = function() {
            vm.dialogRef.close(null);
        };

        vm.save = function() {
            vm.editMachineFormGroup.markAsPristine();
            widgetContext.rxjs.forkJoin([
                saveMachine(),
                saveAttributes()
            ]).subscribe(
                function() {
                    widgetContext.updateAliases();
                    vm.dialogRef.close(null);
                }
            );
        };

        function loadMachine() {
            deviceService.getDevice(entityId.id).subscribe(
                function(device) {
                    vm.device = device;
                    vm.machineType = device.type === 'Haul truck' ? 'Truck' : 'Excavator';

                    vm.editMachineFormGroup.patchValue({
                        machineName: device.name
                    }, { emitEvent: false });

                    // Load attributes based on machine type
                    loadAttributes();
                }
            );
        }

        function loadAttributes() {
            let attributeKeys = [];
            if (vm.machineType === 'Excavator') {
                attributeKeys = ['hydraulicPressureWarningThreshold', 'hydraulicPressureCriticalThreshold'];
            } else {
                attributeKeys = ['loadWeightWarningThreshold', 'loadWeightCriticalThreshold'];
            }

            attributeService.getEntityAttributes(entityId, 'SERVER_SCOPE', attributeKeys).subscribe(
                function(attributes) {
                    let warningValue = 0;
                    let criticalValue = 0;

                    for (let i = 0; i < attributes.length; i++) {
                        const attr = attributes[i];
                        if (attr.key === 'hydraulicPressureWarningThreshold' || attr.key === 'loadWeightWarningThreshold') {
                            warningValue = attr.value;
                        } else if (attr.key === 'hydraulicPressureCriticalThreshold' || attr.key === 'loadWeightCriticalThreshold') {
                            criticalValue = attr.value;
                        }
                    }

                    if (vm.machineType === 'Excavator') {
                        warningValue = widgetContext.unitService.convertUnitValue(warningValue, pressureUnit);
                        criticalValue = widgetContext.unitService.convertUnitValue(criticalValue, pressureUnit);
                    } else {
                        warningValue = widgetContext.unitService.convertUnitValue(warningValue, loadWeightUnit);
                        criticalValue = widgetContext.unitService.convertUnitValue(criticalValue, loadWeightUnit);
                    }

                    vm.editMachineFormGroup.patchValue({
                        warningThreshold: Math.round(warningValue),
                        criticalThreshold: Math.round(criticalValue)
                    }, { emitEvent: false });
                }
            );
        }

        function saveMachine() {
            const formValues = vm.editMachineFormGroup.value;
            vm.device.name = formValues.machineName;
            return deviceService.saveDevice(vm.device);
        }

        function saveAttributes() {
            const formValues = vm.editMachineFormGroup.value;
            const warningValue = formValues.warningThreshold;
            const criticalValue = formValues.criticalThreshold;

            let attributesArray = [];

            if (vm.machineType === 'Excavator') {
                const pressureToStorage = {
                    from: vm.pressureUnitSymbol,
                    METRIC: 'psi',
                    IMPERIAL: 'psi',
                    HYBRID: 'psi'
                };
                const warningInPsi = widgetContext.unitService.convertUnitValue(warningValue, pressureToStorage);
                const criticalInPsi = widgetContext.unitService.convertUnitValue(criticalValue, pressureToStorage);

                attributesArray.push({ key: 'hydraulicPressureWarningThreshold', value: warningInPsi });
                attributesArray.push({ key: 'hydraulicPressureCriticalThreshold', value: criticalInPsi });
            } else {
                const weightToStorage = {
                    from: vm.weightUnitSymbol,
                    METRIC: 'lb',
                    IMPERIAL: 'lb',
                    HYBRID: 'lb'
                };
                const warningInLb = widgetContext.unitService.convertUnitValue(warningValue, weightToStorage);
                const criticalInLb = widgetContext.unitService.convertUnitValue(criticalValue, weightToStorage);

                attributesArray.push({ key: 'loadWeightWarningThreshold', value: warningInLb });
                attributesArray.push({ key: 'loadWeightCriticalThreshold', value: criticalInLb });
            }

            return attributeService.saveEntityAttributes(entityId, 'SERVER_SCOPE', attributesArray);
        }
    }
}

export function editZoneName(widgetContext, entityId, entityName) {
    let $injector = widgetContext.$scope.$injector;
    let customDialog = $injector.get(widgetContext.servicesMap.get('customDialog'));
    let assetService = $injector.get(widgetContext.servicesMap.get('assetService'));

    openEditZoneDialog();

    function openEditZoneDialog() {

        const editZoneHtmlTemplate = '<form #editEntityForm="ngForm" [formGroup]="editZoneFormGroup"\n' +
                                     '      (ngSubmit)="save()" class="edit-entity-form" style="width: 360px;">\n' +
                                     '  <mat-toolbar class="flex flex-row" color="primary">\n' +
                                     '    <h2>Edit zone name</h2>\n' +
                                     '    <span class="flex-1"></span>\n' +
                                     '    <button mat-icon-button (click)="cancel()" type="button">\n' +
                                     '      <mat-icon class="material-icons">close</mat-icon>\n' +
                                     '    </button>\n' +
                                     '  </mat-toolbar>\n' +
                                     '  <mat-progress-bar color="warn" mode="indeterminate" *ngIf="isLoading$ | async">\n' +
                                     '  </mat-progress-bar>\n' +
                                     '  <div style="height: 4px;" *ngIf="!(isLoading$ | async)"></div>\n' +
                                     '  <div mat-dialog-content class="flex flex-col">\n' +
                                     '    <mat-form-field class="mat-block" appearance="outline">\n' +
                                     '      <mat-label>Zone name</mat-label>\n' +
                                     '      <input matInput formControlName="zoneName" required>\n' +
                                     '      <mat-error *ngIf="editZoneFormGroup.get(\'zoneName\').hasError(\'required\')">\n' +
                                     '        Zone name is required.\n' +
                                     '      </mat-error>\n' +
                                     '    </mat-form-field>\n' +
                                     '  </div>\n' +
                                     '  <div mat-dialog-actions class="flex flex-row items-center justify-end">\n' +
                                     '    <button mat-button color="primary"\n' +
                                     '            type="button"\n' +
                                     '            [disabled]="(isLoading$ | async)"\n' +
                                     '            (click)="cancel()" cdkFocusInitial>\n' +
                                     '      Cancel\n' +
                                     '    </button>\n' +
                                     '    <button mat-button mat-raised-button color="primary"\n' +
                                     '            type="submit"\n' +
                                     '            [disabled]="(isLoading$ | async) || editZoneFormGroup.invalid || !editZoneFormGroup.dirty">\n' +
                                     '      Save\n' +
                                     '    </button>\n' +
                                     '  </div>\n' +
                                     '</form>';

        customDialog.customDialog(editZoneHtmlTemplate, EditZoneDialogController).subscribe();
    }

    function EditZoneDialogController(instance) {
        let vm = instance;

        vm.zone = null;

        vm.editZoneFormGroup = vm.fb.group({
            zoneName: ['', [vm.validators.required]]
        });

        loadZone();

        vm.cancel = function() {
            vm.dialogRef.close(null);
        };

        vm.save = function() {
            vm.editZoneFormGroup.markAsPristine();
            saveZone().subscribe(
                function() {
                    widgetContext.updateAliases();
                    vm.dialogRef.close(null);
                }
            );
        };

        function loadZone() {
            assetService.getAsset(entityId.id).subscribe(
                function(asset) {
                    vm.zone = asset;
                    vm.editZoneFormGroup.patchValue({
                        zoneName: vm.zone.name
                    }, { emitEvent: false });
                }
            );
        }

        function saveZone() {
            const formValues = vm.editZoneFormGroup.value;
            vm.zone.name = formValues.zoneName;
            return assetService.saveAsset(vm.zone);
        }
    }
}

export function geoStatusToColor(value) {
    let color = '#f16362';
    if (typeof value !== undefined) {
        if (value === "insideRestrictedZone") {
            color = '#DD2C00;#6F1600';
        }
        if (value === "insideUnloadingZone") {
            color = '#1EBB4D;#0F5E27';
        }
        if (value === "insideLoadingZone") {
            color = '#3388FF;#1A4480';
        }
        if (value === "outsideMineSite") {
            color = '#6D6D6D;#373737';
        }
        if (value === "insideMineSite") {
            color = '#A6BDB0;#434d47'
        }
    }
    return color;
}

export function geoStatusToStatus(value) {
    let status = 'Off site';
    if (typeof value !== undefined) {
        if (value === "insideRestrictedZone") {
            status = 'Restricted';
        }
        if (value === "insideUnloadingZone") {
            status = 'Unloading';
        }
        if (value === "insideLoadingZone") {
            status = 'Loading';
        }
        if (value === "outsideMineSite") {
            status = 'Off site';
        }
        if (value === "insideMineSite") {
            status = 'On site'
        }
    }
    return status;
}

export function zoneTypeToColor(value) {
    let color = '#f16362';
    if (typeof value !== undefined) {
        if (value === "Restricted") {
            color = '#DD2C00';
        }
        if (value === "Unloading") {
            color = '#1EBB4D';
        }
        if (value === "Loading") {
            color = '#3388FF';
        }
    }
    return color;
}

export function markerLabel(data) {
    let textColor = data.geoStatusColor.split(';')[1];
    return `<div style="position: relative; white-space: nowrap; text-align: center;">
              <div style="border: 1px solid ${textColor}; border-radius: 8px; background-color: #ffffff;padding: 8px;">
                    <span style="font-size: 14px;font-weight: 500;line-height: 20px;color: ${textColor}"> ${data.entityName}</span><br />
                    <span style="font-size: 12px;font-weight: 400;line-height: 16px;color: ${textColor}">Fuel level: ${data.fuelLevel}%</span>
              </div>
            </div>`;
}

export function getMachineTooltip(ctx, data, noAction) {
    const speedUnit = {
        from: 'mph',
        METRIC: 'km/h',
        IMPERIAL: 'mph',
        HYBRID: 'km/h'
    };
    const usrSpeedUnit = ctx.unitService.getTargetUnitSymbol(speedUnit);
    const speedValue = ctx.unitService.convertUnitValue(data?.speed || 0, usrSpeedUnit).toFixed(1);

    const machineType = data?.deviceType || data?.type;
    const isTruck = machineType === 'Haul truck';

    let machineSpecificContent = '';

    if (isTruck) {
        const loadWeightUnit = {
            from: 'lb',
            METRIC: 't',
            IMPERIAL: 'lb',
            HYBRID: 't'
        };
        const usrLoadWeightUnit = ctx.unitService.getTargetUnitSymbol(loadWeightUnit);
        const loadWeightValue = ctx.unitService.convertUnitValue(data?.loadWeight || 0, loadWeightUnit).toFixed(1);

        machineSpecificContent = `
        <div>
            <b style="font-weight: 600; color: #333;">Load weight:</b> ${loadWeightValue} ${usrLoadWeightUnit}
        </div>`;
    } else {
        const pressureUnit = {
            from: 'psi',
            METRIC: 'bar',
            IMPERIAL: 'psi',
            HYBRID: 'bar'
        };
        const usrPressureUnit = ctx.unitService.getTargetUnitSymbol(pressureUnit);
        const pressureValue = ctx.unitService.convertUnitValue(data?.hydraulicPressure || 0, pressureUnit).toFixed(1);

        machineSpecificContent = `
        <div>
            <b style="font-weight: 600; color: #333;">Hydraulic pressure:</b> ${pressureValue} ${usrPressureUnit}
        </div>`;
    }

    return `
    <div style="
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
    ">
        <span style="
            font-weight: bold;
            font-size: 16px;
        ">${data?.entityName || ''}</span>
    </div>
    <div style="
        font-size: 14px;
        line-height: 1.8;
        color: #555;
        margin-bottom: 20px
    ">
        <div>
            <b style="font-weight: 600; color: #333;">Latitude:</b> ${data?.latitude.toFixed(4) || ''}
        </div>
        <div>
            <b style="font-weight: 600; color: #333;">Longitude:</b> ${data?.longitude.toFixed(4) || ''}
        </div>
        <div>
            <b style="font-weight: 600; color: #333;">Fuel level:</b> ${data?.fuelLevel || 0} %
        </div>
        <div>
            <b style="font-weight: 600; color: #333;">Speed:</b> ${speedValue} ${usrSpeedUnit}
        </div>
        ${machineSpecificContent}
    </div>
    ${noAction ? '' : "<button-act name='machine-details'>See details</button-act>"}
`;
}