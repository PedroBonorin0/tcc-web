package br.ufjf.tcc.controller;

import java.util.List;

import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zul.Checkbox;

import br.ufjf.tcc.business.PermissaoBusiness;
import br.ufjf.tcc.business.TipoUsuarioBusiness;
import br.ufjf.tcc.model.Permissao;
import br.ufjf.tcc.model.TipoUsuario;
import br.ufjf.tcc.model.Usuario;

public class GerenciamentoPermissoesController extends CommonsController {
	private List<Permissao> permissions = new PermissaoBusiness().getAll();
	private TipoUsuarioBusiness tipoUsuarioBusiness = new TipoUsuarioBusiness();
	private List<TipoUsuario> types = tipoUsuarioBusiness.getAllWithPermissions();
	private TipoUsuario selectedType = types.get(0);

	
	@Init
	public void init() {
		int tipoUsuario = getUsuario().getTipoUsuario().getIdTipoUsuario();
		if(tipoUsuario != Usuario.ADMINISTRADOR) {
			redirectHome();
			return;
		}
		
	}
	
	public List<TipoUsuario> getTypes() {
		return types;
	}

	public List<Permissao> getPermissions() {
		return permissions;
	}

	public void setSelectedType(TipoUsuario selectedType) {
		this.selectedType = selectedType;
	}

	public TipoUsuario getSelectedType() {
		return selectedType;
	}

	@Command("verify")
	public void verify(@BindingParam("permissionName") String permissionName,
			@BindingParam("checkbox") Checkbox checkbox) {
		for (Permissao aux : selectedType.getPermissoes())
			if (aux.getNomePermissao().equals(permissionName)) {
				checkbox.setChecked(true);
				break;
			}

	}

	@Command("edit")
	public void edit(@BindingParam("permission") Permissao permission,
			@BindingParam("checkbox") Checkbox checkbox) {
		if (checkbox.isChecked())
			selectedType.getPermissoes().add(permission);
		else {
			for (Permissao aux : selectedType.getPermissoes())
				if (aux.getNomePermissao()
						.equals(permission.getNomePermissao())) {
					selectedType.getPermissoes().remove(aux);
					break;
				}
		}

		tipoUsuarioBusiness.editar(selectedType);

	}

	@Command("refreshPermissionsList")
	public void RefreshPermissionsList() {
		BindUtils.postNotifyChange(null, null, this, "permissions");
	}

}
